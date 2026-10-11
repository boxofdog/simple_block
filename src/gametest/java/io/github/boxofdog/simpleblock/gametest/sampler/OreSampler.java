package io.github.boxofdog.simpleblock.gametest.sampler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Worldgen measurement tool for Milestone 02 (ORE-01, ORE-02, GEO-01). Not part of the mod.
 *
 * <p>Runs only when the {@code simpleblock.oreSampler} system property names an output directory
 * ({@code ./gradlew runOreSampler}). On a dedicated server with a normal (not superflat) world, it
 * finds sample regions by surface biome, generates them fully, counts every block of rock and ore,
 * writes CSV files and PNG slices, then stops the server. It uses only vanilla and Fabric API
 * classes, so the same file runs unchanged in a pre-change checkout as the control.
 *
 * <p>Rock is "eligible" when ores can sit in it: stone-depth rock (stone, granite, diorite,
 * andesite and the stone ores) and deepslate-depth rock (deepslate, tuff and the deepslate ores).
 * Raw ore blocks of large veins count as rock of their depth. Densities are reported per 100,000
 * blocks of eligible rock, per surface-biome category, so mountains and plains are comparable.
 */
public final class OreSampler implements ModInitializer {
	static final Logger LOGGER = LoggerFactory.getLogger("simple_block-ore-sampler");
	private static final int REGION_CHUNKS = 5;
	private static final int REGION_BLOCKS = REGION_CHUNKS * 16;

	enum Category {
		ORDINARY, DEEP_OCEAN, MOUNTAIN, PLACER, OCEAN, OTHER;

		String id() {
			return this.name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	enum Host {
		STONE, DEEPSLATE
	}

	private static final Set<String> DEEP_OCEAN = Set.of("deep_ocean", "deep_cold_ocean", "deep_frozen_ocean", "deep_lukewarm_ocean");
	private static final Set<String> OCEAN = Set.of("ocean", "cold_ocean", "frozen_ocean", "lukewarm_ocean", "warm_ocean");
	// Same lists as the mod's biome tags simple_block:banded_iron_deposits / placer_iron_deposits.
	private static final Set<String> MOUNTAIN = Set.of("meadow", "frozen_peaks", "jagged_peaks", "stony_peaks", "snowy_slopes",
		"cherry_grove", "windswept_hills", "windswept_forest", "windswept_gravelly_hills", "grove");
	private static final Set<String> PLACER = Set.of("desert", "badlands", "eroded_badlands", "wooded_badlands", "river", "frozen_river");
	private static final Set<String> NOT_ORDINARY = Set.of("beach", "snowy_beach", "stony_shore", "mushroom_fields",
		"dripstone_caves", "lush_caves", "deep_dark", "sulfur_caves");

	static Category categorize(Holder<Biome> biome) {
		String path = biome.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
		if (DEEP_OCEAN.contains(path)) {
			return Category.DEEP_OCEAN;
		}
		if (OCEAN.contains(path)) {
			return Category.OCEAN;
		}
		if (MOUNTAIN.contains(path)) {
			return Category.MOUNTAIN;
		}
		if (PLACER.contains(path)) {
			return Category.PLACER;
		}
		return NOT_ORDINARY.contains(path) ? Category.OTHER : Category.ORDINARY;
	}

	@Override
	public void onInitialize() {
		String out = System.getProperty("simpleblock.oreSampler", "");
		if (out.isBlank()) {
			return;
		}
		ServerLifecycleEvents.SERVER_STARTED.register(server -> server.execute(() -> {
			Path dir = Path.of(out);
			try {
				new Run(server, dir, System.getProperty("simpleblock.oreSampler.label", "unlabelled")).run();
			} catch (Throwable t) {
				LOGGER.error("Ore sampler failed", t);
				try {
					StringWriter trace = new StringWriter();
					t.printStackTrace(new PrintWriter(trace));
					Files.createDirectories(dir);
					Files.writeString(dir.resolve("sampler-error.txt"), trace.toString());
				} catch (IOException ignored) {
				}
			} finally {
				server.halt(false);
			}
		}));
	}

	/** Counts for one (region, category, host) cell. */
	static final class Counts {
		long rock;
		long iron;
		long rawIron;
		long ironExposed;
		long diamond;
		long diamondExposed;
		long copper;
		long rawCopper;
		long removed;

		void add(Counts other) {
			this.rock += other.rock;
			this.iron += other.iron;
			this.rawIron += other.rawIron;
			this.ironExposed += other.ironExposed;
			this.diamond += other.diamond;
			this.diamondExposed += other.diamondExposed;
			this.copper += other.copper;
			this.rawCopper += other.rawCopper;
			this.removed += other.removed;
		}
	}

	record Region(String name, Category wanted, int centerChunkX, int centerChunkZ) {
		int minX() {
			return (this.centerChunkX - REGION_CHUNKS / 2) * 16;
		}

		int minZ() {
			return (this.centerChunkZ - REGION_CHUNKS / 2) * 16;
		}
	}

	static final class Run {
		private final MinecraftServer server;
		private final ServerLevel level;
		private final Path dir;
		private final String label;
		private final long seed;
		private final StringBuilder regionsCsv = new StringBuilder();
		private final StringBuilder countsCsv = new StringBuilder();
		private final StringBuilder depositsCsv = new StringBuilder();
		/** y -> category -> {rock, iron, diamond, stone, deepslate, removed} */
		private final Map<Integer, Map<Category, long[]>> profile = new TreeMap<>();
		private final Map<Block, Host> hosts = new HashMap<>();
		private final Set<Block> ironOres = Set.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
		private final Set<Block> diamondOres = Set.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
		private final Set<Block> copperOres = Set.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
		private final Set<Block> removedRocks = Set.of(Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF);

		Run(MinecraftServer server, Path dir, String label) {
			this.server = server;
			this.level = server.overworld();
			this.dir = dir;
			this.label = label;
			this.seed = this.level.getSeed();
			for (Block block : List.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.INFESTED_STONE,
				Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE, Blocks.GOLD_ORE, Blocks.REDSTONE_ORE, Blocks.EMERALD_ORE,
				Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE)) {
				this.hosts.put(block, Host.STONE);
			}
			for (Block block : List.of(Blocks.DEEPSLATE, Blocks.TUFF, Blocks.INFESTED_DEEPSLATE, Blocks.DEEPSLATE_COAL_ORE,
				Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
				Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_DIAMOND_ORE)) {
				this.hosts.put(block, Host.DEEPSLATE);
			}
		}

		void run() throws IOException {
			Files.createDirectories(this.dir);
			long started = System.nanoTime();
			LOGGER.info("Ore sampler: seed {} label {}", this.seed, this.label);
			List<Region> regions = this.findRegions();
			this.regionsCsv.append("seed,label,region,wanted_category,center_x,center_z,surface_biome_at_center\n");
			this.countsCsv.append("seed,label,region,category,host,columns,rock,iron_ore,raw_iron_block,iron_exposed,diamond_ore,diamond_exposed,copper_ore,raw_copper_block,removed_rock\n");
			this.depositsCsv.append("seed,label,region,kind,count,best_chunk_x,best_chunk_z,best_chunk_count,example_x,example_y,example_z\n");
			for (Region region : regions) {
				this.sample(region);
			}
			this.sampleTrialChambers();
			this.sampleVeins();
			String prefix = this.seed + "-" + this.label;
			Files.writeString(this.dir.resolve(prefix + "-regions.csv"), this.regionsCsv, StandardCharsets.UTF_8);
			Files.writeString(this.dir.resolve(prefix + "-counts.csv"), this.countsCsv, StandardCharsets.UTF_8);
			Files.writeString(this.dir.resolve(prefix + "-deposits.csv"), this.depositsCsv, StandardCharsets.UTF_8);
			StringBuilder profileCsv = new StringBuilder("seed,label,y,category,rock,iron,diamond,stone,deepslate,removed\n");
			this.profile.forEach((y, byCategory) -> byCategory.forEach((category, v) -> profileCsv.append(this.seed).append(',')
				.append(this.label).append(',').append(y).append(',').append(category.id()).append(',').append(v[0]).append(',')
				.append(v[1]).append(',').append(v[2]).append(',').append(v[3]).append(',').append(v[4]).append(',').append(v[5]).append('\n')));
			Files.writeString(this.dir.resolve(prefix + "-profile.csv"), profileCsv, StandardCharsets.UTF_8);
			LOGGER.info("Ore sampler: seed {} done in {} s ({} regions)", this.seed, (System.nanoTime() - started) / 1_000_000_000L, regions.size());
		}

		private List<Region> findRegions() {
			int[][] origins = {{0, 0}, {2500, 0}, {0, 2500}, {-2500, 0}, {0, -2500}, {2500, 2500}, {-2500, -2500}, {2500, -2500}, {-2500, 2500}};
			Map<Category, Integer> wanted = new LinkedHashMap<>();
			wanted.put(Category.ORDINARY, 4);
			wanted.put(Category.DEEP_OCEAN, 2);
			wanted.put(Category.MOUNTAIN, 3);
			wanted.put(Category.PLACER, 3);
			List<Region> regions = new ArrayList<>();
			for (Map.Entry<Category, Integer> entry : wanted.entrySet()) {
				Category category = entry.getKey();
				int found = 0;
				for (int[] origin : origins) {
					if (found >= entry.getValue()) {
						break;
					}
					Predicate<Holder<Biome>> test = biome -> categorize(biome) == category;
					// Mountains are found near their peaks; everything else at sea level.
					int y = category == Category.MOUNTAIN ? 160 : 63;
					Pair<BlockPos, Holder<Biome>> hit = this.level.findClosestBiome3d(test, new BlockPos(origin[0], y, origin[1]), 2400, 32, 64);
					if (hit == null) {
						continue;
					}
					BlockPos pos = hit.getFirst();
					int chunkX = pos.getX() >> 4;
					int chunkZ = pos.getZ() >> 4;
					boolean overlaps = regions.stream().anyMatch(r -> Math.abs(r.centerChunkX() - chunkX) < REGION_CHUNKS * 2
						&& Math.abs(r.centerChunkZ() - chunkZ) < REGION_CHUNKS * 2);
					if (overlaps) {
						continue;
					}
					regions.add(new Region(category.id() + "_" + found, category, chunkX, chunkZ));
					found++;
				}
			}
			return regions;
		}

		private void sample(Region region) throws IOException {
			long started = System.nanoTime();
			LevelChunk[][] chunks = new LevelChunk[REGION_CHUNKS][REGION_CHUNKS];
			for (int cx = 0; cx < REGION_CHUNKS; cx++) {
				for (int cz = 0; cz < REGION_CHUNKS; cz++) {
					chunks[cx][cz] = this.level.getChunk(region.centerChunkX() - REGION_CHUNKS / 2 + cx, region.centerChunkZ() - REGION_CHUNKS / 2 + cz);
				}
			}
			int minX = region.minX();
			int minZ = region.minZ();
			int minY = this.level.getMinY();
			int maxY = this.level.getMaxY();
			Map<Category, Map<Host, Counts>> counts = new LinkedHashMap<>();
			Map<Category, Integer> columns = new LinkedHashMap<>();
			Category[][] columnCategory = new Category[REGION_BLOCKS][REGION_BLOCKS];
			int[][] surface = new int[REGION_BLOCKS][REGION_BLOCKS];
			// Deposit evidence per chunk: high-elevation iron in mountains, shallow iron in placer biomes, iron by lava.
			long[][] highIron = new long[REGION_CHUNKS][REGION_CHUNKS];
			long[][] shallowIron = new long[REGION_CHUNKS][REGION_CHUNKS];
			long[][] lavaIron = new long[REGION_CHUNKS][REGION_CHUNKS];
			int[] highExample = null;
			int[] shallowExample = null;
			int[] lavaExample = null;
			long[] ironByZ = new long[REGION_BLOCKS];
			long[] highIronByZ = new long[REGION_BLOCKS];
			long[] shallowIronByZ = new long[REGION_BLOCKS];
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

			for (int dx = 0; dx < REGION_BLOCKS; dx++) {
				for (int dz = 0; dz < REGION_BLOCKS; dz++) {
					int x = minX + dx;
					int z = minZ + dz;
					LevelChunk chunk = chunks[dx >> 4][dz >> 4];
					int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
					surface[dx][dz] = top;
					Category category = categorize(this.level.getBiome(pos.set(x, top, z)));
					columnCategory[dx][dz] = category;
					columns.merge(category, 1, Integer::sum);
					int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15);
					Map<Host, Counts> byHost = counts.computeIfAbsent(category, c -> new LinkedHashMap<>());
					for (int y = minY; y <= Math.min(top, maxY); y++) {
						BlockState state = this.blockAt(chunks, minX, minZ, x, y, z);
						if (state == null) {
							continue;
						}
						Block block = state.getBlock();
						Host host = this.hosts.get(block);
						if (block == Blocks.RAW_IRON_BLOCK || block == Blocks.RAW_COPPER_BLOCK) {
							host = y < 0 ? Host.DEEPSLATE : Host.STONE;
						}
						if (host == null) {
							continue;
						}
						Counts c = byHost.computeIfAbsent(host, h -> new Counts());
						long[] profileRow = this.profile.computeIfAbsent(y, k -> new TreeMap<>()).computeIfAbsent(category, k -> new long[6]);
						c.rock++;
						profileRow[0]++;
						if (block == Blocks.STONE) {
							profileRow[3]++;
						} else if (block == Blocks.DEEPSLATE) {
							profileRow[4]++;
						}
						if (this.removedRocks.contains(block)) {
							c.removed++;
							profileRow[5]++;
						} else if (this.ironOres.contains(block)) {
							c.iron++;
							profileRow[1]++;
							ironByZ[dz]++;
							if (this.exposed(chunks, minX, minZ, x, y, z)) {
								c.ironExposed++;
							}
							if (category == Category.MOUNTAIN && y >= 64) {
								highIron[dx >> 4][dz >> 4]++;
								highIronByZ[dz]++;
								if (highExample == null) {
									highExample = new int[] {x, y, z};
								}
							}
							if (category == Category.PLACER && y >= floor - 7) {
								shallowIron[dx >> 4][dz >> 4]++;
								shallowIronByZ[dz]++;
								if (shallowExample == null) {
									shallowExample = new int[] {x, y, z};
								}
							}
							if (this.nearLava(chunks, minX, minZ, x, y, z)) {
								lavaIron[dx >> 4][dz >> 4]++;
								if (lavaExample == null) {
									lavaExample = new int[] {x, y, z};
								}
							}
						} else if (block == Blocks.RAW_IRON_BLOCK) {
							c.rawIron++;
						} else if (this.diamondOres.contains(block)) {
							c.diamond++;
							profileRow[2]++;
							if (this.exposed(chunks, minX, minZ, x, y, z)) {
								c.diamondExposed++;
							}
						} else if (this.copperOres.contains(block)) {
							c.copper++;
						} else if (block == Blocks.RAW_COPPER_BLOCK) {
							c.rawCopper++;
						}
					}
				}
			}

			int centerTop = surface[REGION_BLOCKS / 2][REGION_BLOCKS / 2];
			String centerBiome = this.level.getBiome(new BlockPos(minX + REGION_BLOCKS / 2, centerTop, minZ + REGION_BLOCKS / 2))
				.unwrapKey().map(key -> key.identifier().toString()).orElse("?");
			this.regionsCsv.append(this.seed).append(',').append(this.label).append(',').append(region.name()).append(',')
				.append(region.wanted().id()).append(',').append(minX + REGION_BLOCKS / 2).append(',').append(minZ + REGION_BLOCKS / 2)
				.append(',').append(centerBiome).append('\n');
			counts.forEach((category, byHost) -> byHost.forEach((host, c) -> this.countsCsv.append(this.seed).append(',')
				.append(this.label).append(',').append(region.name()).append(',').append(category.id()).append(',')
				.append(host.name().toLowerCase(java.util.Locale.ROOT)).append(',').append(columns.getOrDefault(category, 0)).append(',')
				.append(c.rock).append(',').append(c.iron).append(',').append(c.rawIron).append(',').append(c.ironExposed).append(',')
				.append(c.diamond).append(',').append(c.diamondExposed).append(',').append(c.copper).append(',').append(c.rawCopper)
				.append(',').append(c.removed).append('\n')));
			this.deposit(region, "mountain_iron_y64_plus", highIron, highExample);
			this.deposit(region, "placer_shallow_iron", shallowIron, shallowExample);
			this.deposit(region, "lava_adjacent_iron", lavaIron, lavaExample);

			// Slice through the row that best shows this region's deposit type.
			long[] rowScore = switch (region.wanted()) {
				case MOUNTAIN -> highIronByZ;
				case PLACER -> shallowIronByZ;
				default -> ironByZ;
			};
			int bestZ = REGION_BLOCKS / 2;
			for (int dz = 0; dz < REGION_BLOCKS; dz++) {
				if (rowScore[dz] > rowScore[bestZ]) {
					bestZ = dz;
				}
			}
			// The centre row is the same in every run, so mod and control slices compare directly.
			this.writeSlice(region, chunks, minX, minZ, REGION_BLOCKS / 2, "slice");
			this.writeSlice(region, chunks, minX, minZ, bestZ, "deposit");
			this.writeMap(region, chunks, minX, minZ, surface, columnCategory);
			LOGGER.info("Ore sampler: {} at chunk {},{} ({}) sampled in {} ms", region.name(), region.centerChunkX(), region.centerChunkZ(),
				centerBiome, (System.nanoTime() - started) / 1_000_000L);
		}

		private void deposit(Region region, String kind, long[][] perChunk, int[] example) {
			long total = 0;
			int bestX = 0;
			int bestZ = 0;
			for (int cx = 0; cx < REGION_CHUNKS; cx++) {
				for (int cz = 0; cz < REGION_CHUNKS; cz++) {
					total += perChunk[cx][cz];
					if (perChunk[cx][cz] > perChunk[bestX][bestZ]) {
						bestX = cx;
						bestZ = cz;
					}
				}
			}
			int chunkX = region.centerChunkX() - REGION_CHUNKS / 2 + bestX;
			int chunkZ = region.centerChunkZ() - REGION_CHUNKS / 2 + bestZ;
			this.depositsCsv.append(this.seed).append(',').append(this.label).append(',').append(region.name()).append(',').append(kind)
				.append(',').append(total).append(',').append(chunkX).append(',').append(chunkZ).append(',').append(perChunk[bestX][bestZ])
				.append(',').append(example == null ? "" : example[0]).append(',').append(example == null ? "" : example[1]).append(',')
				.append(example == null ? "" : example[2]).append('\n');
		}

		private BlockState blockAt(LevelChunk[][] chunks, int minX, int minZ, int x, int y, int z) {
			int dx = x - minX;
			int dz = z - minZ;
			if (dx < 0 || dz < 0 || dx >= REGION_BLOCKS || dz >= REGION_BLOCKS || y < this.level.getMinY() || y > this.level.getMaxY()) {
				return null;
			}
			LevelChunk chunk = chunks[dx >> 4][dz >> 4];
			LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
			return section.getBlockState(x & 15, y & 15, z & 15);
		}

		private boolean exposed(LevelChunk[][] chunks, int minX, int minZ, int x, int y, int z) {
			int[][] offsets = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
			for (int[] o : offsets) {
				BlockState neighbour = this.blockAt(chunks, minX, minZ, x + o[0], y + o[1], z + o[2]);
				if (neighbour != null && (neighbour.isAir() || !neighbour.getFluidState().isEmpty())) {
					return true;
				}
			}
			return false;
		}

		private boolean nearLava(LevelChunk[][] chunks, int minX, int minZ, int x, int y, int z) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = -2; dy <= 2; dy++) {
					for (int dz = -2; dz <= 2; dz++) {
						BlockState state = this.blockAt(chunks, minX, minZ, x + dx, y + dy, z + dz);
						if (state != null && state.is(Blocks.LAVA)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		private static int color(BlockState state) {
			Block block = state.getBlock();
			if (state.isAir()) {
				return 0xFFFFFF;
			}
			if (block == Blocks.WATER) {
				return 0x3F76E4;
			}
			if (block == Blocks.LAVA) {
				return 0xFF6A00;
			}
			if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
				return 0xD81B60;
			}
			if (block == Blocks.RAW_IRON_BLOCK) {
				return 0x8E0038;
			}
			if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
				return 0x00E5FF;
			}
			if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE || block == Blocks.RAW_COPPER_BLOCK) {
				return 0x2E7D32;
			}
			if (block == Blocks.GRANITE) {
				return 0xC77B5F;
			}
			if (block == Blocks.DIORITE) {
				return 0xE8E4D8;
			}
			if (block == Blocks.ANDESITE) {
				return 0x9AA0A6;
			}
			if (block == Blocks.TUFF) {
				return 0x6D6B4E;
			}
			if (block == Blocks.STONE) {
				return 0x7F7F7F;
			}
			if (block == Blocks.DEEPSLATE) {
				return 0x3C3C44;
			}
			return 0xB59A6A;
		}

		private void writeSlice(Region region, LevelChunk[][] chunks, int minX, int minZ, int dz, String kind) throws IOException {
			int minY = this.level.getMinY();
			int height = this.level.getMaxY() - minY + 1;
			int scale = 3;
			BufferedImage image = new BufferedImage(REGION_BLOCKS * scale, height * scale, BufferedImage.TYPE_INT_RGB);
			for (int dx = 0; dx < REGION_BLOCKS; dx++) {
				for (int y = minY; y < minY + height; y++) {
					BlockState state = this.blockAt(chunks, minX, minZ, minX + dx, y, minZ + dz);
					int rgb = state == null ? 0 : color(state);
					int py = (minY + height - 1 - y) * scale;
					for (int sx = 0; sx < scale; sx++) {
						for (int sy = 0; sy < scale; sy++) {
							image.setRGB(dx * scale + sx, py + sy, rgb);
						}
					}
				}
			}
			ImageIO.write(image, "png", this.dir.resolve(this.seed + "-" + this.label + "-" + region.name() + "-" + kind + "-z" + (minZ + dz) + ".png").toFile());
		}

		/**
		 * Top-down map of iron ore per column, black where there is none. Mountain regions count only
		 * iron at y 64 and above, placer regions only iron within 7 blocks of the surface, so the
		 * map shows that region's deposit rather than the background ore.
		 */
		private void writeMap(Region region, LevelChunk[][] chunks, int minX, int minZ, int[][] surface, Category[][] categories)
			throws IOException {
			int scale = 4;
			BufferedImage image = new BufferedImage(REGION_BLOCKS * scale, REGION_BLOCKS * scale, BufferedImage.TYPE_INT_RGB);
			for (int dx = 0; dx < REGION_BLOCKS; dx++) {
				for (int dz = 0; dz < REGION_BLOCKS; dz++) {
					int iron = 0;
					int from = switch (region.wanted()) {
						case MOUNTAIN -> 64;
						case PLACER -> surface[dx][dz] - 8;
						default -> this.level.getMinY();
					};
					for (int y = Math.max(from, this.level.getMinY()); y <= surface[dx][dz]; y++) {
						BlockState state = this.blockAt(chunks, minX, minZ, minX + dx, y, minZ + dz);
						if (state != null && this.ironOres.contains(state.getBlock())) {
							iron++;
						}
					}
					int shade = Math.min(255, 64 + iron * 48);
					boolean inCategory = categories[dx][dz] == region.wanted();
					int rgb = iron == 0 ? (inCategory ? 0x303030 : 0x101010) : (shade << 16) | ((shade / 4) << 8) | (shade / 3);
					for (int sx = 0; sx < scale; sx++) {
						for (int sz = 0; sz < scale; sz++) {
							image.setRGB(dx * scale + sx, dz * scale + sz, rgb);
						}
					}
				}
			}
			ImageIO.write(image, "png", this.dir.resolve(this.seed + "-" + this.label + "-" + region.name() + "-ironmap.png").toFile());
		}

		private boolean insidePiece(StructureStart start, BlockPos pos) {
			for (var piece : start.getPieces()) {
				if (piece.getBoundingBox().isInside(pos)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * GEO-03: large ore veins, whose filler was tuff (iron, y -60..-8) or granite (copper, y 0..50).
		 * Veins are wide noise bands, so a grid of chunks is generated only to the terrain stage (fast)
		 * to find the chunk with the most vein ore of each kind; that chunk and its neighbours are then
		 * generated fully and counted in the vein's y range, and sliced. Vein regions are reported
		 * separately and are not part of the per-category densities.
		 */
		private void sampleVeins() throws IOException {
			StringBuilder csv = new StringBuilder("seed,label,vein,chunk_x,chunk_z,ore,raw_block,tuff,granite,stone,deepslate,other_solid\n");
			int[] bestIron = {0, 0, -1};
			int[] bestCopper = {0, 0, -1};
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int cx = -48; cx <= 48; cx += 6) {
				for (int cz = -48; cz <= 48; cz += 6) {
					var chunk = this.level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.TERRAIN, true);
					int iron = 0;
					int copper = 0;
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							for (int y = -60; y <= 50; y++) {
								Block block = chunk.getBlockState(pos.set((cx << 4) + x, y, (cz << 4) + z)).getBlock();
								if (block == Blocks.RAW_IRON_BLOCK || block == Blocks.DEEPSLATE_IRON_ORE && y <= -8) {
									iron++;
								} else if (block == Blocks.RAW_COPPER_BLOCK || block == Blocks.COPPER_ORE && y >= 0) {
									copper++;
								}
							}
						}
					}
					if (iron > bestIron[2]) {
						bestIron = new int[] {cx, cz, iron};
					}
					if (copper > bestCopper[2]) {
						bestCopper = new int[] {cx, cz, copper};
					}
				}
			}
			for (Object[] vein : new Object[][] {{"iron", bestIron, -60, -8, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK},
				{"copper", bestCopper, 0, 50, Blocks.COPPER_ORE, Blocks.RAW_COPPER_BLOCK}}) {
				int[] best = (int[]) vein[1];
				int minY = (Integer) vein[2];
				int maxY = (Integer) vein[3];
				Region region = new Region("vein_" + vein[0], Category.OTHER, best[0], best[1]);
				long[] counts = new long[7];
				for (int x = region.minX(); x < region.minX() + REGION_BLOCKS; x++) {
					for (int z = region.minZ(); z < region.minZ() + REGION_BLOCKS; z++) {
						for (int y = minY; y <= maxY; y++) {
							BlockState state = this.level.getBlockState(pos.set(x, y, z));
							Block block = state.getBlock();
							int slot = block == vein[4] ? 0 : block == vein[5] ? 1 : block == Blocks.TUFF ? 2 : block == Blocks.GRANITE ? 3
								: block == Blocks.STONE ? 4 : block == Blocks.DEEPSLATE ? 5 : state.isSolid() ? 6 : -1;
							if (slot >= 0) {
								counts[slot]++;
							}
						}
					}
				}
				csv.append(this.seed).append(',').append(this.label).append(',').append(vein[0]).append(',').append(best[0]).append(',')
					.append(best[1]);
				for (long count : counts) {
					csv.append(',').append(count);
				}
				csv.append('\n');
				this.writeVeinSlice(region, minY, maxY);
			}
			Files.writeString(this.dir.resolve(this.seed + "-" + this.label + "-veins.csv"), csv, StandardCharsets.UTF_8);
		}

		private void writeVeinSlice(Region region, int minY, int maxY) throws IOException {
			int scale = 3;
			int height = maxY - minY + 9;
			BufferedImage image = new BufferedImage(REGION_BLOCKS * scale, height * scale, BufferedImage.TYPE_INT_RGB);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int z = region.minZ() + REGION_BLOCKS / 2;
			for (int dx = 0; dx < REGION_BLOCKS; dx++) {
				for (int i = 0; i < height; i++) {
					int y = maxY + 4 - i;
					int rgb = color(this.level.getBlockState(pos.set(region.minX() + dx, y, z)));
					for (int sx = 0; sx < scale; sx++) {
						for (int sy = 0; sy < scale; sy++) {
							image.setRGB(dx * scale + sx, i * scale + sy, rgb);
						}
					}
				}
			}
			ImageIO.write(image, "png", this.dir.resolve(this.seed + "-" + this.label + "-" + region.name() + "-slice-z" + z + ".png").toFile());
		}

		/** GEO-03: the nearest trial chambers keep their layout and working blocks, without tuff. */
		private void sampleTrialChambers() throws IOException {
			Holder<Structure> trialChambers = this.server.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace("trial_chambers")));
			Pair<BlockPos, Holder<Structure>> hit = this.level.getChunkSource().getGenerator()
				.findNearestMapStructure(this.level, HolderSet.direct(trialChambers), BlockPos.ZERO, 64, false);
			StringBuilder csv = new StringBuilder("seed,label,structure,start_x,start_z,block,count\n");
			if (hit != null) {
				BlockPos start = hit.getFirst();
				this.level.getChunk(start.getX() >> 4, start.getZ() >> 4);
				StructureStart structureStart = this.level.structureManager().getStructureAt(start, trialChambers.value());
				if (structureStart.isValid()) {
					BoundingBox box = structureStart.getBoundingBox();
					Map<String, Long> blocks = new TreeMap<>();
					BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
					for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
						for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
							this.level.getChunk(cx, cz);
						}
					}
					for (int x = box.minX(); x <= box.maxX(); x++) {
						for (int z = box.minZ(); z <= box.maxZ(); z++) {
							for (int y = box.minY(); y <= box.maxY(); y++) {
								// Only blocks belonging to a structure piece, not the rock around it.
								pos.set(x, y, z);
								if (!this.insidePiece(structureStart, pos)) {
									continue;
								}
								BlockState state = this.level.getBlockState(pos);
								String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
								if (id.contains("tuff") || id.contains("deepslate") || id.contains("granite") || id.contains("diorite")
									|| id.contains("andesite") || id.equals("trial_spawner") || id.equals("vault") || id.contains("copper_bulb")
									|| id.equals("stone") || id.contains("stone_brick") || id.equals("smooth_stone")) {
									blocks.merge(id, 1L, Long::sum);
								}
							}
						}
					}
					blocks.forEach((id, count) -> csv.append(this.seed).append(',').append(this.label).append(",trial_chambers,")
						.append(start.getX()).append(',').append(start.getZ()).append(',').append(id).append(',').append(count).append('\n'));
					LOGGER.info("Ore sampler: trial chambers at {} with {} counted block types", start, blocks.size());
				}
			}
			Files.writeString(this.dir.resolve(this.seed + "-" + this.label + "-trial-chambers.csv"), csv, StandardCharsets.UTF_8);
		}
	}
}
