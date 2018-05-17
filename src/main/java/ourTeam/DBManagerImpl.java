package ourTeam;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.*;


public class DBManagerImpl implements DBManager{
    AmazonDynamoDB client;

    public DBManagerImpl() {
        client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    }

    public Integer getPartCount(int set, String part) {
        HashMap<String, AttributeValue> queryMap = new HashMap<>();
        AttributeValue setValue = new AttributeValue();
        setValue.setN("" + set);
        queryMap.put("set_id", setValue);
        queryMap.put("part_num", new AttributeValue(part));
        GetItemRequest request = new GetItemRequest().withKey(queryMap).withTableName("inventory_parts").withProjectionExpression("quantity");
        try {
            Object[] resultArray = client.getItem(request).getItem().values().toArray();
            if (resultArray[0] != null) {
                AttributeValue firstResult = (AttributeValue) resultArray[0];
                String toReturn = firstResult.getS();
                if(toReturn == null)
                    toReturn = firstResult.getN();
                return Integer.parseInt(toReturn);
            } else {
                System.out.format("No part found with the ID %s\n", part);
            }
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
        return null;
    }

    public void decrementSet(int set, int amount) {
        int initial = getSetCount(set);
        HashMap<String, AttributeValue> queryMap = new HashMap<>();
        AttributeValue setValue = new AttributeValue();
        setValue.setN("" + set);
        queryMap.put("set_id", setValue);
        HashMap<String, AttributeValueUpdate> updatesMap = new HashMap<>();
        updatesMap.put("quantity", new AttributeValueUpdate(new AttributeValue(initial - amount + ""), AttributeAction.PUT));
        try {
            client.updateItem("inventory_sets", queryMap, updatesMap);
        } catch (AmazonServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Removed " + amount + " of set " + set);
    }

    public Set<String> getParts(int set) {
        HashMap<String, AttributeValue> attrValues = new HashMap<>();
        attrValues.put(":set_id", new AttributeValue().withN("" + set));

        QueryRequest queryReq = new QueryRequest()
                .withTableName("inventory_parts")
                .withKeyConditionExpression("set_id = :set_id")
                .withExpressionAttributeValues(attrValues)
                .withProjectionExpression("part_num");

        try {
            List<Map<String, AttributeValue>> resultList = client.query(queryReq).getItems();
            Set<String> results = new HashSet<>();
            for (Map result : resultList) {
                for (Object value : result.values()) {
                    AttributeValue part = (AttributeValue) value;
                    results.add(part.getS());
                }
            }
            return results;
        } catch (AmazonDynamoDBException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
        return null;
    }

    public void incrementPart(int set, String part, int amount) {
        int initial = getPartCount(set, part);
        HashMap<String, AttributeValue> queryMap = new HashMap<>();
        AttributeValue setValue = new AttributeValue();
        setValue.setN("" + set);
        queryMap.put("set_id", setValue);
        queryMap.put("part_num", new AttributeValue(part));
        HashMap<String, AttributeValueUpdate> updatesMap = new HashMap<>();
        updatesMap.put("quantity", new AttributeValueUpdate(new AttributeValue(initial + amount + ""), AttributeAction.PUT));
        try {
            client.updateItem("inventory_parts", queryMap, updatesMap);
        } catch (AmazonServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Added " + amount + " of part " + part);
    }


    public Integer getSetCount(int set) {
        HashMap<String, AttributeValue> queryMap = new HashMap<>();
        AttributeValue setValue = new AttributeValue();
        setValue.setN("" + set);
        queryMap.put("set_id", setValue);
        GetItemRequest request = new GetItemRequest().withKey(queryMap).withTableName("inventory_sets").withProjectionExpression("quantity");
        try {
            Object[] resultArray = client.getItem(request).getItem().values().toArray();
            if (resultArray[0] != null) {
                AttributeValue firstResult = (AttributeValue) resultArray[0];
                String toReturn = firstResult.getS();
                if(toReturn == null)
                    toReturn = firstResult.getN();
                return Integer.parseInt(toReturn);
            } else {
                System.out.format("No set found with the ID %s\n", set);
            }
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
        return null;
    }

    public void incrementSet(int set, int amount) {
        int initial = getSetCount(set);
        HashMap<String, AttributeValue> queryMap = new HashMap<>();
        AttributeValue setValue = new AttributeValue();
        setValue.setN("" + set);
        queryMap.put("set_id", setValue);
        HashMap<String, AttributeValueUpdate> updatesMap = new HashMap<>();
        updatesMap.put("quantity", new AttributeValueUpdate(new AttributeValue(initial + amount + ""), AttributeAction.PUT));
        try {
            client.updateItem("inventory_sets", queryMap, updatesMap);
        } catch (AmazonServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Added " + amount + " of set " + set);
    }

    public void decrementPart(int set, String part, int amount) {
        int initial = getPartCount(set, part);
        HashMap<String, AttributeValue> queryMap = new HashMap<>();
        AttributeValue setValue = new AttributeValue();
        setValue.setN("" + set);
        queryMap.put("set_id", setValue);
        queryMap.put("part_num", new AttributeValue(part));
        HashMap<String, AttributeValueUpdate> updatesMap = new HashMap<>();
        updatesMap.put("quantity", new AttributeValueUpdate(new AttributeValue(initial - amount + ""), AttributeAction.PUT));
        try {
            client.updateItem("inventory_parts", queryMap, updatesMap);
        } catch (AmazonServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Removed " + amount + " of part " + part);
    }
}
