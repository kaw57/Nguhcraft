package org.nguh.nguhcraft.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.Discord;
import org.nguh.nguhcraft.server.NguhcraftServerPlayer;
import org.nguh.nguhcraft.server.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin extends PlayerEntity implements NguhcraftServerPlayer {
    public ServerPlayerMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique private boolean Vanished = false;
    @Unique private long DiscordId = 0;
    @Unique private int DiscordColour = 0;
    @Unique private String DiscordName = "";
    @Unique private String DiscordAvatar = "";
    @Unique private Text NguhcraftDisplayName = null;

    @Unique static private final String TAG_ROOT = "Nguhcraft";
    @Unique static private final String TAG_VANISHED = "Vanished";
    @Unique static private final String TAG_DISCORD_ID = "DiscordID";
    @Unique static private final String TAG_DISCORD_COLOUR = "DiscordRoleColour";
    @Unique static private final String TAG_DISCORD_NAME = "DiscordName";
    @Unique static private final String TAG_DISCORD_AVATAR = "DiscordAvatar";

    @Override public long getDiscordId() { return DiscordId; }
    @Override public void setDiscordId(long id) { DiscordId = id; }

    @Override public String getDiscordName() { return DiscordName; }
    @Override public void setDiscordName(String name) { DiscordName = name; }

    @Override public int getDiscordColour() { return DiscordColour; }
    @Override public void setDiscordColour(int colour) { DiscordColour = colour; }

    @Override public Text getNguhcraftDisplayName() { return NguhcraftDisplayName; }
    @Override public void setNguhcraftDisplayName(Text name) { NguhcraftDisplayName = name; }

    @Override public String getDiscordAvatarURL() { return DiscordAvatar; }
    @Override public void setDiscordAvatarURL(String url) { DiscordAvatar = url; }

    @Override public boolean isLinked() { return DiscordId != 0; }

    @Override
    public Text getDisplayName() { return NguhcraftDisplayName; }

    @SuppressWarnings("UnreachableCode")
    @Inject(method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    private void loadData(@NotNull NbtCompound nbt, CallbackInfo ci) {
        if (!nbt.contains(TAG_ROOT)) return;
        nbt = nbt.getCompound(TAG_ROOT);
        Vanished = nbt.getBoolean(TAG_VANISHED);
        DiscordId = nbt.getLong(TAG_DISCORD_ID);
        DiscordColour = nbt.getInt(TAG_DISCORD_COLOUR);
        DiscordName = nbt.getString(TAG_DISCORD_NAME);
        DiscordAvatar = nbt.getString(TAG_DISCORD_AVATAR);

        // Compute name component for early messages (e.g. join message).
        var SP = (ServerPlayerEntity) (Object) this;
        NguhcraftDisplayName = DiscordId != Discord.INVALID_ID
            ? Text.literal(getNameForScoreboard())
            : Text.literal(DiscordName).withColor(DiscordColour);
        PlayerList.UpdateCacheEntry(SP);
    }

    @Inject(method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    private void saveData(@NotNull NbtCompound nbt, CallbackInfo ci) {
        var tag = new NbtCompound();
        tag.putBoolean(TAG_VANISHED, Vanished);
        tag.putLong(TAG_DISCORD_ID, DiscordId);
        tag.putInt(TAG_DISCORD_COLOUR, DiscordColour);
        tag.putString(TAG_DISCORD_NAME, DiscordName);
        tag.putString(TAG_DISCORD_AVATAR, DiscordAvatar);
        nbt.put(TAG_ROOT, tag);
    }
}
