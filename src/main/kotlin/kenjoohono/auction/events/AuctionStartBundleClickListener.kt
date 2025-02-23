package kenjoohono.auction.events

import kenjoohono.auction.gui.AuctionStartGui
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.Plugin

class AuctionStartBundleClickListener(private val plugin: Plugin) : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.title != AuctionStartGui.TITLE) return
        // 번들이 있는 슬롯(13번)을 클릭했을 때만 반응
        if (event.rawSlot != 13) return
        val player = event.whoClicked as? Player ?: return
        // 클릭 후, 1틱 뒤에 번들 내용을 확인하여 플레이어에게 전송
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            AuctionStartGui().checkAndSendBundleContents(player)
        }, 1L)
    }
}