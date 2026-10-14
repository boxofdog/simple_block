package io.github.boxofdog.simpleblock.effigy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * An effigy's lower half: which way it faces, as any horizontal angle (the placing player's yaw, in
 * Minecraft's convention: 0 south, 90 west, 180 north, 270 east). Saved and sent to clients; the
 * renderer turns the monument by it and the fear cone ({@link Effigies}) points the same way.
 */
public class EffigyBlockEntity extends BlockEntity {
	private float yaw;

	public EffigyBlockEntity(BlockPos pos, BlockState state) {
		super(EffigyContent.EFFIGY_ENTITY, pos, state);
	}

	public float yaw() {
		return this.yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
		this.setChanged();
	}

	public EffigyType type() {
		return this.getBlockState().getBlock() instanceof EffigyBlock effigy ? effigy.type() : EffigyType.ZOMBIE;
	}

	/** The way the effigy faces, a horizontal unit vector. */
	public Vec3 facing() {
		double radians = Math.toRadians(this.yaw);
		return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putFloat("yaw", this.yaw);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.yaw = input.getFloatOr("yaw", 0.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		this.saveAdditional(output);
		return output.buildResult();
	}
}
