package kenjoohono.auction.gui

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
        for (slot in 0 until 26) {
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
        val itemsJsonList = sortedItems.map { item ->
            val metaMap = item.itemMeta?.serialize()
            buildString {
                append("{\n")
                append("  \"material\": \"${item.type}\",\n")
                append("  \"amount\": ${item.amount}")
                if (metaMap != null && metaMap.isNotEmpty()) {
                    val metaJson = metaMap.toString().replace("\"", "\\\"")
                    append(",\n  \"meta\": \"${metaJson}\"")
                }
                append("\n}")
            }
        }

        val timestamp = Instant.now().toString()
        val jsonOutput = "{\n  \"timestamp\": \"$timestamp\",\n  \"items\": [\n" +
                itemsJsonList.joinToString(separator = ",\n") +
                "\n  ]\n}"
        val plugin = Bukkit.getPluginManager().getPlugin("Auction") ?: return
        val playerFolder = File(plugin.dataFolder, player.uniqueId.toString())
        if (!playerFolder.exists()) playerFolder.mkdirs()
        val existingFiles = playerFolder.listFiles { _, name -> name.endsWith(".json5") } ?: arrayOf()
        val nextNumber = (existingFiles.mapNotNull { it.nameWithoutExtension.toIntOrNull() }.maxOrNull() ?: 0) + 1
        val outputFile = File(playerFolder, "$nextNumber.json5")
        outputFile.writeText(jsonOutput)
        player.sendMessage("${ChatColor.GREEN}번들 정보가 '${outputFile.name}' 파일로 저장되었습니다.")

        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', "&b가격 설정"),
            ChatColor.translateAlternateColorCodes('&', "&7채팅으로 원하는 가격을 입력해주세요"),
            10, 300, 10
        )

        var remainingSeconds = 15
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
                val message = event.message.trim()
                Bukkit.getScheduler().cancelTask(countdownTaskId)
                clearTitlesAndActionBar(player)
                if (message.equals("취소", ignoreCase = true)) {
                    sortedItems.forEach { item ->
                        player.inventory.addItem(item.clone())
                    }
                    HandlerList.unregisterAll(this)
                    player.sendMessage("${ChatColor.YELLOW}경매 등록이 취소되었습니다.")
                } else {
                    try {
                        val price = message.toDouble()
                        val auctionJsonOutput = "{\n  \"timestamp\": \"$timestamp\",\n  \"price\": $price,\n  \"items\": [\n" +
                                itemsJsonList.joinToString(separator = ",\n") +
                                "\n  ]\n}"
                        outputFile.writeText(auctionJsonOutput)
                        HandlerList.unregisterAll(this)
                        player.sendMessage("${ChatColor.GRAY}경매 아이템이 등록되었습니다.")
                    } catch (e: NumberFormatException) {
                        player.sendMessage("${ChatColor.RED}유효한 숫자를 입력해주세요. (취소하려면 '취소' 입력)")
                    }
                }
            }
        }
        Bukkit.getPluginManager().registerEvents(chatListener, plugin)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            HandlerList.unregisterAll(chatListener)
            Bukkit.getScheduler().cancelTask(countdownTaskId)
            clearTitlesAndActionBar(player)
            sortedItems.forEach { item ->
                player.inventory.addItem(item.clone())
            }
            player.sendMessage("${ChatColor.YELLOW}시간 초과로 경매 등록이 취소되었습니다.")
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