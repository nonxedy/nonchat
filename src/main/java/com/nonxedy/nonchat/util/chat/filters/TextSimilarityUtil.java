package com.nonxedy.nonchat.util.chat.filters;

/**
 * Utility class for calculating text similarity
 * Uses Levenshtein distance algorithm to determine how similar two strings are
 */
public class TextSimilarityUtil {

    /**
     * Calculates similarity between two strings using Levenshtein distance
     * Returns a value between 0.0 (completely different) and 1.0 (identical)
     * 
     * @param str1 First string to compare
     * @param str2 Second string to compare
     * @return Similarity score between 0.0 and 1.0
     */
    public static double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        // Normalize strings for comparison
        String normalized1 = normalizeString(str1);
        String normalized2 = normalizeString(str2);

        // If strings are identical after normalization, return 1.0
        if (normalized1.equals(normalized2)) {
            return 1.0;
        }

        // Calculate Levenshtein distance
        int distance = levenshteinDistance(normalized1, normalized2);
        
        // Get maximum length for normalization
        int maxLength = Math.max(normalized1.length(), normalized2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }

        // Convert distance to similarity (0.0 to 1.0)
        // Similarity = 1 - (distance / maxLength)
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Normalizes a string for comparison
     * Converts to lowercase and removes extra whitespace
     * 
     * @param str String to normalize
     * @return Normalized string
     */
    private static String normalizeString(String str) {
        if (str == null) {
            return "";
        }
        // Convert to lowercase and trim
        return str.toLowerCase().trim();
    }

    /**
     * Calculates Levenshtein distance between two strings
     * This is the minimum number of single-character edits needed to transform one string into another
     * 
     * @param str1 First string
     * @param str2 Second string
     * @return Levenshtein distance
     */
    private static int levenshteinDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();

        // Create a matrix to store distances
        int[][] dp = new int[len1 + 1][len2 + 1];

        // Initialize base cases
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        // Fill the matrix
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    // Characters match, no cost
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // Characters don't match, take minimum of three operations:
                    // 1. Insertion
                    // 2. Deletion
                    // 3. Substitution
                    dp[i][j] = 1 + Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    );
                }
            }
        }

        return dp[len1][len2];
    }
}

