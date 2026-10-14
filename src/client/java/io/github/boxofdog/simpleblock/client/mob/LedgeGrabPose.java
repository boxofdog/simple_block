package io.github.boxofdog.simpleblock.client.mob;

/**
 * M09: the skeleton's ledge-grab pose on the client. The skeleton's synced flag
 * ({@code feature/SpiderJockeys.Grabbing}) is copied into its render state, and the model raises both
 * arms straight up, hands at the lip, while it is set.
 */
public final class LedgeGrabPose {
	/** Implemented by the skeleton render state (mixin). */
	public interface Holder {
		boolean simpleBlock$grabbing();

		void simpleBlock$setGrabbing(boolean grabbing);
	}

	private LedgeGrabPose() {
	}
}
