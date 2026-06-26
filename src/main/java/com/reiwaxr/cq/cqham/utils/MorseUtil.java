package com.reiwaxr.cq.cqham.utils;

import java.util.HashMap;
import java.util.Map;

public class MorseUtil {
    // 字符 -> 摩尔斯码
    public static final Map<Character, String> CHAR_TO_MORSE = new HashMap<>();
    // 摩尔斯码 -> 字符（反转映射）
    public static final Map<String, Character> MORSE_TO_CHAR = new HashMap<>();

    static {
        // 字母
        CHAR_TO_MORSE.put('A', ".-");
        CHAR_TO_MORSE.put('B', "-...");
        CHAR_TO_MORSE.put('C', "-.-.");
        CHAR_TO_MORSE.put('D', "-..");
        CHAR_TO_MORSE.put('E', ".");
        CHAR_TO_MORSE.put('F', "..-.");
        CHAR_TO_MORSE.put('G', "--.");
        CHAR_TO_MORSE.put('H', "....");
        CHAR_TO_MORSE.put('I', "..");
        CHAR_TO_MORSE.put('J', ".---");
        CHAR_TO_MORSE.put('K', "-.-");
        CHAR_TO_MORSE.put('L', ".-..");
        CHAR_TO_MORSE.put('M', "--");
        CHAR_TO_MORSE.put('N', "-.");
        CHAR_TO_MORSE.put('O', "---");
        CHAR_TO_MORSE.put('P', ".--.");
        CHAR_TO_MORSE.put('Q', "--.-");
        CHAR_TO_MORSE.put('R', ".-.");
        CHAR_TO_MORSE.put('S', "...");
        CHAR_TO_MORSE.put('T', "-");
        CHAR_TO_MORSE.put('U', "..-");
        CHAR_TO_MORSE.put('V', "...-");
        CHAR_TO_MORSE.put('W', ".--");
        CHAR_TO_MORSE.put('X', "-..-");
        CHAR_TO_MORSE.put('Y', "-.--");
        CHAR_TO_MORSE.put('Z', "--..");
        // 数字
        CHAR_TO_MORSE.put('0', "-----");
        CHAR_TO_MORSE.put('1', ".----");
        CHAR_TO_MORSE.put('2', "..---");
        CHAR_TO_MORSE.put('3', "...--");
        CHAR_TO_MORSE.put('4', "....-");
        CHAR_TO_MORSE.put('5', ".....");
        CHAR_TO_MORSE.put('6', "-....");
        CHAR_TO_MORSE.put('7', "--...");
        CHAR_TO_MORSE.put('8', "---..");
        CHAR_TO_MORSE.put('9', "----.");
        // 常用标点
        CHAR_TO_MORSE.put('.', ".-.-.-");
        CHAR_TO_MORSE.put(',', "--..--");
        CHAR_TO_MORSE.put('?', "..--..");
        CHAR_TO_MORSE.put('/', "-..-.");
        CHAR_TO_MORSE.put('-', "-....-");
        CHAR_TO_MORSE.put('_', "..--.-");
        CHAR_TO_MORSE.put('=', "-...-");
        CHAR_TO_MORSE.put(':', "---...");
        CHAR_TO_MORSE.put(';', "-.-.-.");
        CHAR_TO_MORSE.put('(', "-.--.");
        CHAR_TO_MORSE.put(')', "-.--.-");
        CHAR_TO_MORSE.put('+', ".-.-.");
        CHAR_TO_MORSE.put('@', ".--.-.");
        CHAR_TO_MORSE.put('!', "-.-.--");
        CHAR_TO_MORSE.put('"', ".-..-.");

        // 反向映射填充
        for (Map.Entry<Character, String> entry : CHAR_TO_MORSE.entrySet()) {
            MORSE_TO_CHAR.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * 文本转摩尔斯码
     * @param text 输入英文字母/数字/标点，空格分隔单词
     * @param charSep 字符间隔符
     * @param wordSep 单词间隔符
     * @return 摩尔斯码字符串
     */
    public static String textToMorse(String text, String charSep, String wordSep) {
        if (text == null || text.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        String upper = text.toUpperCase();
        String[] words = upper.split("\\s+");
        for (int wIdx = 0; wIdx < words.length; wIdx++) {
            String word = words[wIdx];
            for (int cIdx = 0; cIdx < word.length(); cIdx++) {
                char c = word.charAt(cIdx);
                String morse = CHAR_TO_MORSE.get(c);
                if (morse != null) {
                    sb.append(morse);
                }
                // 字符间加分隔符，非最后一个字符
                if (cIdx != word.length() - 1) {
                    sb.append(charSep);
                }
            }
            // 单词间隔
            if (wIdx != words.length - 1) {
                sb.append(wordSep);
            }
        }
        return sb.toString();
    }

    /**
     * 摩尔斯码转回文本
     * @param morse 点划码，字符间空格分隔，单词双空格分隔
     * @param charSep 字符分隔符
     * @param wordSep 单词分隔符
     * @return 原始文本
     */
    public static String morseToText(String morse, String charSep, String wordSep) {
        if (morse == null || morse.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        // 按单词分割
        String[] wordMorseArr = morse.split(wordSep);
        for (String wordMorse : wordMorseArr) {
            String[] charMorseArr = wordMorse.split(charSep);
            for (String cm : charMorseArr) {
                cm = cm.trim();
                if (MORSE_TO_CHAR.containsKey(cm)) {
                    result.append(MORSE_TO_CHAR.get(cm));
                }
            }
            result.append(" ");
        }
        return result.toString().trim();
    }
}