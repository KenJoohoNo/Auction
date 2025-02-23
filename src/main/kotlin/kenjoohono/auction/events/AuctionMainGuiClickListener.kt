package kenjoohono.auction.events

import kenjoohono.auction.gui.AuctionMainGui
import kenjoohono.auction.gui.AuctionStartGui
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class AuctionMainGuiClickListener : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title
        if (title.startsWith(AuctionMainGui.TITLE)) {
            event.isCancelled = true
            val item = event.currentItem ?: return
            if (!item.hasItemMeta() || !item.itemMeta.hasDisplayName()) return
            val displayName = item.itemMeta.displayName
            val auctionStart = ChatColor.translateAlternateColorCodes('&', "&a경매 등록")
            val nextPage = ChatColor.translateAlternateColorCodes('&', "&a다음 페이지")
            val prevPage = ChatColor.translateAlternateColorCodes('&', "&6이전 페이지")
            val player = event.whoClicked as? Player ?: return
            val currentPage = title.substringAfter("(페이지: ").substringBefore(")").toIntOrNull() ?: 0

            when (displayName) {
                auctionStart -> AuctionStartGui().openAuctionStartGui(player)
                nextPage -> AuctionMainGui().openAuctionGui(player, currentPage + 1)
                prevPage -> AuctionMainGui().openAuctionGui(player, currentPage - 1)
            }
        } else if (event.view.title == AuctionStartGui.TITLE) {
            if (event.rawSlot < event.view.topInventory.size) {
                event.isCancelled = (event.rawSlot != 13)
            } else {
                event.isCancelled = false
            }
        }
    }
}