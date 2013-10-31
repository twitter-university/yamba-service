package com.twitter.university.android.yamba.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ProjectionMap {

    /**
     * Builder
     */
    public static class Builder {
        Map<String, String> colMap = new HashMap<String, String>();

        /**
         * @param virtCol the virtual column name
         * @param actCol the actual column name
         * @return the builder
         */
        public Builder addColumn(String virtCol, String actCol) {
            colMap.put(virtCol, actCol + " AS " + virtCol);
            return this;
        }

        /**
         * @param virtCol the virtual column name
         * @param actTable the target table
         * @param actCol the actual column name
         * @return the builder
         */
        public Builder addColumn(String virtCol, String actTable, String actCol) {
            return addColumn(virtCol, actTable + "." + actCol);
        }

        /**
         * @return the column map
         */
        public ProjectionMap build() { return new ProjectionMap(colMap); }
    }


    private final Map<String, String> colMap;

    ProjectionMap(Map<String, String> colMap) {
        this.colMap = Collections.unmodifiableMap(colMap);
    }

    /**
     * @return the projection map
     */
    public Map<String, String> getProjectionMap() { return colMap; }
}
