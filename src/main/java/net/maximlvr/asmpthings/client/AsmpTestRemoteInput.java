package net.maximlvr.asmpthings.client;

import net.maximlvr.asmpthings.network.payload.AsmpTestInputPayload;
import net.minecraft.client.player.Input;

/** Incoming controls are separate from the keyboard state sent to the other player. */
public final class AsmpTestRemoteInput {
    private AsmpTestInputPayload latest;
    private long receivedAt;

    public void accept(AsmpTestInputPayload input, long tick) { latest = input; receivedAt = tick; }
    public void clear() { latest = null; }
    public AsmpTestInputPayload current(long tick) {
        return latest != null && tick >= receivedAt && tick - receivedAt < 10 ? latest : null;
    }

    public void apply(Input input, long tick, float slowdown) {
        var remote = current(tick);
        input.forwardImpulse = remote == null ? 0 : Math.clamp(remote.forward(), -1, 1) * slowdown;
        input.leftImpulse = remote == null ? 0 : Math.clamp(remote.left(), -1, 1) * slowdown;
        input.up = input.forwardImpulse > 0;
        input.down = input.forwardImpulse < 0;
        input.left = input.leftImpulse > 0;
        input.right = input.leftImpulse < 0;
        input.jumping = remote != null && remote.has(AsmpTestInputPayload.JUMP);
        input.shiftKeyDown = remote != null && remote.has(AsmpTestInputPayload.SNEAK);
    }
}
