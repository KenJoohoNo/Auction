package kenjoohono.auction.gui

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class AuctionMainGui {
    fun openAuctionGui(player: Player) {
        val auctionInventory: Inventory = Bukkit.createInventory(null, 54, "경매")
        player.openInventory(auctionInventory)
    }
}