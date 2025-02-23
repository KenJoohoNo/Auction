package kenjoohono.auction.events

import kenjoohono.auction.gui.AuctionMainGui
import kenjoohono.auction.gui.AuctionStartGui
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class AuctionMainGuiClickListener : Listener {
    private val ongoingActions = ConcurrentHashMap<String, String>()

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title ?: return
        val player = event.whoClicked as? Player ?: return

        if (title.contains("${AuctionMainGui.TITLE} (페이지:")) {
            if (event.rawSlot in 0..44) {
                event.isCancelled = true
                val clickedItem = event.currentItem ?: return
                val meta = clickedItem.itemMeta ?: return
                val lore = meta.lore ?: return
                if (lore.size < 3) return

                val rawItemId = lore.getOrNull(0) ?: ""
                val itemId = ChatColor.stripColor(rawItemId)?.trim()?.removePrefix("아이템 아이디 : ")?.trim() ?: ""

                val rawRegistrant = lore.getOrNull(1) ?: ""
                val registrant = ChatColor.stripColor(rawRegistrant)?.trim()?.removePrefix("아이템 등록자 : ")?.trim() ?: ""

                val rawPrice = lore.getOrNull(2) ?: ""
                val priceStr = ChatColor.stripColor(rawPrice)?.trim()?.removePrefix("아이템 가격 : ")?.trim() ?: ""
                val price = priceStr.toDoubleOrNull() ?: 0.0

                val isSeller = player.name.equals(registrant, ignoreCase = true)
                val actionKeyword = if (isSeller) "회수" else "구매"

                if (ongoingActions.containsKey(itemId)) {
                    player.sendMessage("${ChatColor.RED}현재 다른 사용자가 '${itemId}' 아이템을 ${if (isSeller) "구매" else "회수"} 중입니다.")
                    return
                }

                if (!isSeller) {
                    val balanceStr = PlaceholderAPI.setPlaceholders(player, "%cmi_user_balance%")
                    val balance = balanceStr.toDoubleOrNull() ?: 0.0
                    if (balance < price) {
                        player.sendMessage("${ChatColor.RED}잔액이 부족합니다. 현재 잔액: $balance, 가격: $price")
                        return
                    }
                }

                player.sendMessage("${ChatColor.YELLOW}해당 아이템 (ID: $itemId)을 $actionKeyword 하시려면 채팅에 '$actionKeyword'라고 입력해주세요. (15초 이내)")
                ongoingActions[itemId] = player.name
                var actionCompleted = false
                val plugin = Bukkit.getPluginManager().getPlugin("Auction") ?: return

                val countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                    player.sendActionBar(ChatColor.translateAlternateColorCodes('&', "&7'$actionKeyword' 입력 대기 중..."))
                }, 0L, 20L).taskId

                val chatListener = object : Listener {
                    @EventHandler
                    fun onPlayerChat(event: AsyncPlayerChatEvent) {
                        if (event.player != player) return
                        event.isCancelled = true
                        val message = event.message.trim()
                        if (message.equals(actionKeyword, ignoreCase = true)) {
                            Bukkit.getScheduler().cancelTask(countdownTaskId)
                            clearTitlesAndActionBar(player)
                            HandlerList.unregisterAll(this)
                            actionCompleted = true
                            player.closeInventory()
                            ongoingActions.remove(itemId)
                            processAuctionAction(player, itemId, registrant, price, actionKeyword, clickedItem)
                        }
                    }
                }
                Bukkit.getPluginManager().registerEvents(chatListener, plugin)
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    if (!actionCompleted) {
                        HandlerList.unregisterAll(chatListener)
                        Bukkit.getScheduler().cancelTask(countdownTaskId)
                        clearTitlesAndActionBar(player)
                        player.sendMessage("${ChatColor.YELLOW}시간 초과로 $actionKeyword 가 취소되었습니다.")
                        ongoingActions.remove(itemId)
                    }
                }, 15 * 20L)
            } else {
                event.isCancelled = true
            }
        } else if (title == AuctionStartGui.TITLE) {
            event.isCancelled = event.clickedInventory == event.view.topInventory && event.rawSlot != 13
        }
    }

    private fun processAuctionAction(
        player: Player,
        itemId: String,
        registrant: String,
        price: Double,
        action: String,
        clickedItem: ItemStack
    ) {
        if (action.equals("구매", ignoreCase = true)) {
            val intPrice = price.toInt()
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "money take ${player.name} $intPrice")
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "money give $registrant $intPrice")
        } else if (action.equals("회수", ignoreCase = true)) {
            val meta = clickedItem.itemMeta
            if (meta != null) {
                meta.lore = null
                clickedItem.itemMeta = meta
            }
        }

        deleteAuctionFile(itemId, registrant)
        player.sendMessage("${ChatColor.GREEN}[ID: $itemId] 아이템이 성공적으로 $action 되었습니다.")
        giveItemToPlayer(player, clickedItem)
    }

    private fun deleteAuctionFile(itemId: String, registrant: String) {
        val plugin = Bukkit.getPluginManager().getPlugin("Auction") ?: return
        val sellerFolder = File(plugin.dataFolder, registrant)
        if (sellerFolder.exists() && sellerFolder.isDirectory) {
            val auctionFile = File(sellerFolder, "$itemId.json5")
            if (auctionFile.exists()) {
                auctionFile.delete()
            }
        }
    }

    private fun giveItemToPlayer(player: Player, item: ItemStack) {
        val leftovers = player.inventory.addItem(item)
        leftovers.values.forEach { player.world.dropItem(player.location, it) }
    }

    private fun clearTitlesAndActionBar(player: Player) {
        player.sendTitle("", "", 0, 0, 0)
        player.sendActionBar("")
    }
}