package kenjoohono.auction.events

import kenjoohono.auction.gui.AuctionMainGui
import kenjoohono.auction.gui.AuctionStartGui
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class AuctionMainGuiClickListener : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.title == AuctionMainGui.TITLE) {
            event.isCancelled = true
            val item = event.currentItem ?: return
            if (!item.hasItemMeta() || !item.itemMeta.hasDisplayName()) return
            val displayName = item.itemMeta.displayName
            val auctionItem = ChatColor.translateAlternateColorCodes('&', "&a경매 등록")
            if (displayName == auctionItem) {
                val player = event.whoClicked as? Player ?: return
                AuctionStartGui().openAuctionStartGui(player)
            }
        } else if (event.view.title == AuctionStartGui.TITLE) {
            if (event.rawSlot < event.view.topInventory.size) {
                val cursorItem = event.cursor
                event.isCancelled = cursorItem == null || cursorItem.type == Material.AIR
            } else {
                event.isCancelled = false
            }
        }
    }
}