package dev.caecorthus.sparktraits.impl;

public final class AssassinRolePage {
    private AssassinRolePage() {
    }

    public static int pageSizeForHeight(int startY, int controlY, int columns, int itemHeight, int rowGap) {
        int availableHeight = Math.max(0, controlY - startY - 10);
        int rowStride = itemHeight + rowGap;
        int rows = Math.max(1, (availableHeight + rowGap) / rowStride);
        return Math.max(1, columns) * rows;
    }

    public static Layout layout(int itemCount, int requestedPage, int pageSize) {
        int safeItemCount = Math.max(0, itemCount);
        int safePageSize = Math.max(1, pageSize);
        int pageCount = Math.max(1, (safeItemCount + safePageSize - 1) / safePageSize);
        int page = Math.clamp(requestedPage, 0, pageCount - 1);
        int startIndex = Math.min(safeItemCount, page * safePageSize);
        int endIndex = Math.min(safeItemCount, startIndex + safePageSize);
        return new Layout(page, pageCount, startIndex, endIndex);
    }

    public record Layout(int page, int pageCount, int startIndex, int endIndex) {
        public boolean hasPrevious() {
            return page > 0;
        }

        public boolean hasNext() {
            return page + 1 < pageCount;
        }

        public int visibleCount() {
            return endIndex - startIndex;
        }
    }
}
