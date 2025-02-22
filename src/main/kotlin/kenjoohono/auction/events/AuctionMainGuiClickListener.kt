package kenjoohono.auction.events

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import kenjoohono.auction.gui.AuctionMainGui

class AuctionMainGuiClickListener : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.title == AuctionMainGui.TITLE) {
            event.isCancelled = true
        }
    }
}