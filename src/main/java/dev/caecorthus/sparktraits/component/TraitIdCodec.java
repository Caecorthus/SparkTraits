package dev.caecorthus.sparktraits.component;

import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;

/** Shared trait-id list codecs for component NBT, component sync packets and the SparkTraitsApi trait snapshot.
 *  Writers keep iteration order; readers drop unparsable and retired ids ({@link RetiredTraitIds}) and add into the
 *  caller's set.
 *  组件 NBT、组件同步包与 SparkTraitsApi 天赋快照共用的天赋 id 列表编解码。写入保持迭代顺序；读取丢弃无法解析与已退役的 id
 *  （{@link RetiredTraitIds}），并加入调用方提供的集合。 */
public final class TraitIdCodec {
    private TraitIdCodec() {
    }

    public static NbtList toNbt(Collection<Identifier> ids) {
        NbtList list = new NbtList();
        for (Identifier id : ids) {
            list.add(NbtString.of(id.toString()));
        }
        return list;
    }

    /** Adds without clearing; callers that replace state clear the set first.
     *  只添加不清空；需要整体替换的调用方先自行清空集合。 */
    public static void fromNbt(NbtList list, Set<Identifier> ids) {
        for (int i = 0; i < list.size(); i++) {
            Identifier id = Identifier.tryParse(list.getString(i));
            if (id != null && !RetiredTraitIds.isRetired(id)) {
                ids.add(id);
            }
        }
    }

    static void writeIdentifierSet(RegistryByteBuf buf, Collection<Identifier> ids) {
        buf.writeVarInt(ids.size());
        for (Identifier id : ids) {
            buf.writeString(id.toString());
        }
    }

    /** Clears the target, then reads the full declared count so the packet layout stays aligned.
     *  先清空目标集合，再按声明数量完整读取，保证协议布局对齐。 */
    static void readIdentifierSet(RegistryByteBuf buf, Set<Identifier> ids) {
        ids.clear();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            Identifier id = Identifier.tryParse(buf.readString());
            if (id != null && !RetiredTraitIds.isRetired(id)) {
                ids.add(id);
            }
        }
    }
}
