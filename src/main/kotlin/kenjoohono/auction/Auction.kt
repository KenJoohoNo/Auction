package kenjoohono.auction

import org.bukkit.plugin.java.JavaPlugin
import kenjoohono.auction.commands.AuctionCommand
import kenjoohono.auction.events.AuctionMainGuiClickListener
import kenjoohono.auction.events.AuctionStartGuiClose
import kenjoohono.auction.events.AuctionStartBundleClickListener

class Auction : JavaPlugin() {

    override fun onEnable() {
        getCommand("경매")?.setExecutor(AuctionCommand())

        // events
        server.pluginManager.registerEvents(AuctionMainGuiClickListener(), this)
        server.pluginManager.registerEvents(AuctionStartGuiClose(this), this)
        server.pluginManager.registerEvents(AuctionStartBundleClickListener(this), this)
    }
}
