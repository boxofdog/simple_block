package io.github.boxofdog.simpleblock.client.coot;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.coot.OldCoot;
import io.github.boxofdog.simpleblock.coot.OldCootContent;

/** M14 the Old Coot, drawn with his iron pickaxe in hand ({@link OldCootModel}). */
public class OldCootRenderer extends MobRenderer<OldCoot, ArmedEntityRenderState, OldCootModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "textures/entity/old_coot.png");

	public OldCootRenderer(EntityRendererProvider.Context context) {
		super(context, new OldCootModel(context.bakeLayer(OldCootModel.LAYER)), 0.45F);
		this.addLayer(new ItemInHandLayer<>(this));
	}

	public static void register() {
		ModelLayerRegistry.registerModelLayer(OldCootModel.LAYER, OldCootModel::createBodyLayer);
		EntityRendererRegistry.register(OldCootContent.OLD_COOT, OldCootRenderer::new);
	}

	@Override
	public ArmedEntityRenderState createRenderState() {
		return new ArmedEntityRenderState();
	}

	@Override
	public void extractRenderState(OldCoot coot, ArmedEntityRenderState state, float partialTicks) {
		super.extractRenderState(coot, state, partialTicks);
		ArmedEntityRenderState.extractArmedEntityRenderState(coot, state, this.itemModelResolver, partialTicks);
	}

	@Override
	public Identifier getTextureLocation(ArmedEntityRenderState state) {
		return TEXTURE;
	}
}
