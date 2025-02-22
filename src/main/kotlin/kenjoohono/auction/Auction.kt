package kenjoohono.auction

import org.bukkit.plugin.java.JavaPlugin
import kenjoohono.auction.commands.AuctionCommand
import kenjoohono.auction.events.AuctionMainGuiClickListener

class Auction : JavaPlugin() {

    override fun onEnable() {
        getCommand("경매")?.setExecutor(AuctionCommand())

        // events
        server.pluginManager.registerEvents(AuctionMainGuiClickListener(), this)
    }
}
