package io.github.boxofdog.simpleblock.feature;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * Old identifiers of renamed Simple Block content, and the ones that replaced them, so that worlds and
 * items saved before a rename load as the renamed content ({@code mixin/compat/MappedRegistryMixin}
 * looks the old name up as the new one in every registry; vanilla's registries once had the same
 * aliases). M08 playtest (owner, 2026-10-13): the Pre-Industrial Blast Furnace is the Blast Furnace,
 * blocks, item, block entity and menu alike.
 */
public final class LegacyIds {
	private static final Map<Identifier, Identifier> RENAMED = Map.of(
		id("pre_industrial_blast_furnace"), id("blast_furnace"),
		id("pre_industrial_blast_furnace_part"), id("blast_furnace_part"),
		id("pre_industrial_blast_furnace_channel"), id("blast_furnace_channel"),
		id("pre_industrial_blast_furnace_mold"), id("blast_furnace_mold"));

	private LegacyIds() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path);
	}

	/** The identifier {@code id} was renamed to, or null if it was not renamed. */
	public static @Nullable Identifier renamed(@Nullable Identifier id) {
		return id == null ? null : RENAMED.get(id);
	}
}
