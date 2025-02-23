package kenjoohono.auction.gui

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BundleMeta
import java.io.File
import java.time.Instant

class AuctionStartGui {

    fun openAuctionStartGui(player: Player) {
        val inventory = Bukkit.createInventory(null, 27, TITLE)
        val blackGlass = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }
        for (slot in 0 until 27) {
            inventory.setItem(slot, blackGlass)
        }
        val bundleItem = ItemStack(Material.BLACK_BUNDLE).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a등록할 아이템을 올려주세요"))
            }
        }
        inventory.setItem(13, bundleItem)
        auctionBundle = inventory
        player.openInventory(inventory)
    }

    fun checkAndSendBundleContents(player: Player) {
        val inventory = auctionBundle ?: return
        val bundleItem = inventory.getItem(13) ?: return
        if (bundleItem.type != Material.BLACK_BUNDLE) return
        val bundleMeta = bundleItem.itemMeta as? BundleMeta ?: return
        val items = bundleMeta.getItems()
        if (items.isEmpty()) return
        val sortedItems = items.map { it.clone() }.sortedBy { it.type.name }
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val itemsList = sortedItems.map { item ->
            val metaMap = item.itemMeta?.serialize()
            val itemMap = mutableMapOf<String, Any>(
                "material" to item.type.toString(),
                "amount" to item.amount
            )
            if (metaMap != null && metaMap.isNotEmpty()) {
                itemMap["meta"] = metaMap
            }
            itemMap
        }
        val timestamp = Instant.now().toString()
        val auctionData = mutableMapOf<String, Any>(
            "timestamp" to timestamp,
            "playerName" to player.name,
            "items" to itemsList
        )
        player.sendMessage("${ChatColor.GREEN}경매 아이템이 준비되었습니다. 가격을 입력해주세요.")
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', "&b가격 설정"),
            ChatColor.translateAlternateColorCodes('&', "&7채팅으로 원하는 가격을 입력해주세요"),
            10, 300, 10
        )
        var remainingSeconds = 15
        var auctionRegistered = false
        val plugin = Bukkit.getPluginManager().getPlugin("Auction") ?: return
        val countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (remainingSeconds > 0) {
                val actionbarMsg = ChatColor.translateAlternateColorCodes(
                    '&',
                    "&7취소를 원한다면 &c취소 &7라고 채팅에 입력해주세요 &f(제한 시간 : ${remainingSeconds}초)"
                )
                player.sendActionBar(actionbarMsg)
                remainingSeconds--
            }
        }, 0L, 20L).taskId
        val chatListener = object : Listener {
            @EventHandler(priority = EventPriority.HIGHEST)
            fun onPlayerChat(event: AsyncPlayerChatEvent) {
                if (event.player != player) return
                event.isCancelled = true
                try {
                    val message = event.message.trim()
                    Bukkit.getScheduler().cancelTask(countdownTaskId)
                    clearTitlesAndActionBar(player)
                    HandlerList.unregisterAll(this)
                    if (message.equals("취소", ignoreCase = true)) {
                        player.sendMessage("${ChatColor.YELLOW}경매 등록이 취소되었습니다.")
                        auctionRegistered = false
                    } else {
                        val price = message.toDouble()
                        auctionData["price"] = price
                        val playerFolder = File(plugin.dataFolder, player.uniqueId.toString())
                        if (!playerFolder.exists()) playerFolder.mkdirs()
                        val existingFiles = playerFolder.listFiles { _, name -> name.endsWith(".json5") } ?: arrayOf()
                        val nextNumber = (existingFiles.mapNotNull { it.nameWithoutExtension.toIntOrNull() }.maxOrNull() ?: 0) + 1
                        val outputFile = File(playerFolder, "$nextNumber.json5")
                        outputFile.writeText(gson.toJson(auctionData))
                        player.sendMessage("${ChatColor.GRAY}경매 아이템이 등록되었습니다.")
                        auctionRegistered = true
                    }
                } catch (e: Exception) {
                    player.sendMessage("${ChatColor.RED}오류가 발생했습니다. 다시 시도해주세요.")
                }
            }
        }
        Bukkit.getPluginManager().registerEvents(chatListener, plugin)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            try {
                if (!auctionRegistered) {
                    HandlerList.unregisterAll(chatListener)
                    Bukkit.getScheduler().cancelTask(countdownTaskId)
                    clearTitlesAndActionBar(player)
                    player.sendMessage("${ChatColor.YELLOW}시간 초과로 경매 등록이 취소되었습니다.")
                }
            } catch (e: Exception) {
            }
        }, 15 * 20L)
        player.closeInventory()
    }

    private fun clearTitlesAndActionBar(player: Player) {
        player.sendTitle("", "", 0, 0, 0)
        player.sendActionBar("")
    }

    companion object {
        const val TITLE = "경매 등록"
        var auctionBundle: Inventory? = null
    }
}