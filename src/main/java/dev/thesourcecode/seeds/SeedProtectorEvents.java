package dev.thesourcecode.seeds;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Instant;
import java.util.Map;

public class SeedProtectorEvents implements Listener {
    private Map<Player, Instant> cropMessage;

    public SeedProtectorEvents(Map<Player, Instant> cropMessage) {
        this.cropMessage = cropMessage;
    }

    @EventHandler
    private void farmBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Player player = event.getPlayer();

        if (player.isSneaking()) {
            return;
        }
        if (isNotCrop(block)) {
            return;
        }

        final Ageable ageable = (Ageable) block.getState().getBlockData();

        if (ageable.getAge() == ageable.getMaximumAge()) {
            autoReplant(player, block);
            spawnParticles(block.getLocation());
        } else {
            final Instant now = Instant.now();
            cropMessage.compute(player, (uuid, instant) -> {
                if (instant != null && now.isBefore(instant)) {
                    return instant;
                }
                player.sendMessage("§6[SP] §eSneak to break baby seeds.");
                return now.plusSeconds(10);
            });
        }

        event.setCancelled(true);
    }

    @EventHandler
    private void cropTrample(PlayerInteractEvent event){
        if(event.getAction() != Action.PHYSICAL) {return;}
        if(!event.hasBlock()) {return;}

        final Block farmland = event.getClickedBlock();
        if(farmland == null) return;
        final Block crop = farmland.getRelative(BlockFace.UP);
        if(isNotCrop(crop)) {return;}
        event.setCancelled(true);
    }

    private void spawnParticles(Location location) {
        location.add(.5, .5, .5);
        location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, location, 10, .5, .5, .5, 0);
    }

    private void autoReplant(Player player, Block block) {
        block.getDrops(player.getInventory().getItemInMainHand()).forEach(drop -> {
            switch (drop.getType()) {
                case WHEAT_SEEDS:
                case BEETROOT_SEEDS:
                case CARROTS:
                case POTATOES:
                case NETHER_WART:
                    drop.setAmount(drop.getAmount() - 1);
                    break;
            }

            final Location blockLocation = block.getLocation();
            blockLocation.getWorld().dropItem(blockLocation,drop);
        });

        block.setType(block.getType());
    }

    private boolean isNotCrop(Block block) {
        switch (block.getType()) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case MELON_STEM:
            case PUMPKIN_STEM:
            case NETHER_WART:
                return false;
        }
        return true;
    }
}
