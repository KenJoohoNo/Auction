package kenjoohono.auction.gui

import org.bukkit.Bukkit
import org.bukkit.entity.Player

class AuctionStartGui {
    fun openAuctionStartGui(player: Player) {
        val auctionInventory = Bukkit.createInventory(null, 27, TITLE)
        player.openInventory(auctionInventory)
    }

    companion object {
        const val TITLE = "경매 등록"
    }
}