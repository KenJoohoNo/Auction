package kenjoohono.auction.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kenjoohono.auction.gui.AuctionMainGui

class AuctionCommand : CommandExecutor {
    private val auctionMainGui = AuctionMainGui()
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { return true }
        auctionMainGui.openAuctionGui(sender)
        return true
    }
}