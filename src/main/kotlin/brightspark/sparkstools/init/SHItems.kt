package brightspark.sparkstools.init

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.item.CustomTool
import brightspark.sparkstools.item.SHToolItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import net.minecraft.item.Item
import net.minecraftforge.registries.IForgeRegistry
import java.io.FileReader

object SHItems {
	fun init(registry: IForgeRegistry<Item>) {
		Gson().fromJson<List<CustomTool>>(
			JsonReader(FileReader(SparksTools.customToolsFile)),
			object : TypeToken<List<CustomTool>>() {}.type
		).forEach { registry.register(SHToolItem(it)) }
	}
}