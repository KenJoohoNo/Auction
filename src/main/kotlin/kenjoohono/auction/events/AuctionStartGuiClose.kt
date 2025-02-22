package kenjoohono.auction.events

import kenjoohono.auction.gui.AuctionMainGui
import kenjoohono.auction.gui.AuctionStartGui
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.*

class AuctionStartGuiClose(private val plugin: Plugin) : Listener {
    private val closeGui = mutableMapOf<UUID, BukkitTask>()

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.view.title == AuctionStartGui.TITLE) {
            val player = event.player as? Player ?: return
            closeGui[player.uniqueId]?.cancel()
            closeGui[player.uniqueId] = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (player.openInventory.title != AuctionStartGui.TITLE) {
                    AuctionMainGui().openAuctionGui(player)
                }
                closeGui.remove(player.uniqueId)
            }, 1L)
        }
    }
}