package org.bukkit.craftbukkit.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.server.ChatClickable;
import net.minecraft.server.ChatComponentText;
import net.minecraft.server.ChatModifier;
import net.minecraft.server.EnumChatFormat;
import net.minecraft.server.EnumClickAction;
import net.minecraft.server.IChatBaseComponent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public final class CraftChatMessage {
    private static class StringMessage {
        private static final Map<Character, EnumChatFormat> formatMap;
        private static final Pattern INCREMENTAL_PATTERN = Pattern.compile("(" + String.valueOf(org.bukkit.ChatColor.COLOR_CHAR) + "[0-9a-fk-or])|(\\n)|(?:(https?://[^ ][^ ]*?)(?=[\\.\\?!,;:]?(?:[ \\n]|$)))", Pattern.CASE_INSENSITIVE);

        static {
            Builder<Character, EnumChatFormat> builder = ImmutableMap.builder();
            for (EnumChatFormat format : EnumChatFormat.values()) {
                builder.put(Character.toLowerCase(format.toString().charAt(1)), format);
            }
            formatMap = builder.build();
        }

        private final List<IChatBaseComponent> list = new ArrayList<IChatBaseComponent>();
        private IChatBaseComponent currentChatComponent = new ChatComponentText("");
        private ChatModifier modifier = new ChatModifier();
        private final IChatBaseComponent[] output;
        private int currentIndex;
        private final String message;

        private StringMessage(String message,  boolean keepNewlines) {
            this.message = message;
            if (message == null) {
                output = new IChatBaseComponent[] { currentChatComponent };
                return;
            }
            list.add(currentChatComponent);

            Matcher matcher = INCREMENTAL_PATTERN.matcher(message);
            String match = null;
            while (matcher.find()) {
                int groupId = 0;
                while ((match = matcher.group(++groupId)) == null) {
                    // NOOP
                }
                appendNewComponent(matcher.start(groupId));
                switch (groupId) {
                case 1:
                    EnumChatFormat format = formatMap.get(match.toLowerCase().charAt(1));
                    if (format == EnumChatFormat.RESET) {
                        modifier = new ChatModifier();
                    } else if (format.isFormat()) {
                        switch (format) {
                        case BOLD:
                            modifier.setBold(Boolean.TRUE);
                            break;
                        case ITALIC:
                            modifier.setItalic(Boolean.TRUE);
                            break;
                        case STRIKETHROUGH:
                            modifier.setStrikethrough(Boolean.TRUE);
                            break;
                        case UNDERLINE:
                            modifier.setUnderline(Boolean.TRUE);
                            break;
                        case OBFUSCATED:
                            modifier.setRandom(Boolean.TRUE);
                            break;
                        default:
                            throw new AssertionError("Unexpected message format");
                        }
                    } else { // Color resets formatting
                        modifier = new ChatModifier().setColor(format);
                    }
                    break;
                case 2:
                    if (keepNewlines) {
                        currentChatComponent.addSibling(new ChatComponentText("\n"));
                    } else {
                        currentChatComponent = null;
                    }
                    break;
                case 3:
                    modifier.setChatClickable(new ChatClickable(EnumClickAction.OPEN_URL, match));
                    appendNewComponent(matcher.end(groupId));
                    modifier.setChatClickable((ChatClickable) null);
                }
                currentIndex = matcher.end(groupId);
            }

            if (currentIndex < message.length()) {
                appendNewComponent(message.length());
            }

            output = list.toArray(new IChatBaseComponent[0]);
        }

        private void appendNewComponent(int index) {
            if (index <= currentIndex) {
                return;
            }
            IChatBaseComponent addition = new ChatComponentText(message.substring(currentIndex, index)).setChatModifier(modifier);
            currentIndex = index;
            modifier = modifier.clone();
            if (currentChatComponent == null) {
                currentChatComponent = new ChatComponentText("");
                list.add(currentChatComponent);
            }
            currentChatComponent.addSibling(addition);
        }

        private IChatBaseComponent[] getOutput() {
            return output;
        }
    }

    public static IChatBaseComponent[] fromString(String message) {
        return fromString(message, false);
    }
    
    public static IChatBaseComponent[] fromString(String message, boolean keepNewlines) {
        return new StringMessage(message, keepNewlines).getOutput();
    }
    
    public static String fromComponent(IChatBaseComponent component) {
        if (component == null) return "";
        StringBuilder out = new StringBuilder();
        
        for (IChatBaseComponent c : (Iterable<IChatBaseComponent>) component) {
            ChatModifier modi = c.getChatModifier();
            out.append(modi.getColor() == null ? EnumChatFormat.BLACK : modi.getColor());
            if (modi.isBold()) {
                out.append(EnumChatFormat.BOLD);
            }
            if (modi.isItalic()) {
                out.append(EnumChatFormat.ITALIC);
            }
            if (modi.isUnderlined()) {
                out.append(EnumChatFormat.UNDERLINE);
            }
            if (modi.isStrikethrough()) {
                out.append(EnumChatFormat.STRIKETHROUGH);
            }
            if (modi.isRandom()) {
                out.append(EnumChatFormat.OBFUSCATED);
            }
            out.append(c.getText());
        }
        return out.toString().replaceFirst("^(§0)*", "");
    }

    private CraftChatMessage() {
    }
}
