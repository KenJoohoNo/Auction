package kenjoohono.auction

import org.bukkit.plugin.java.JavaPlugin
import kenjoohono.auction.commands.AuctionCommand

class Auction : JavaPlugin() {

    override fun onEnable() {
        getCommand("경매")?.setExecutor(AuctionCommand())
    }
}
