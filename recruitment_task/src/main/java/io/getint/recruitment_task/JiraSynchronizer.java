package io.getint.recruitment_task;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class JiraSynchronizer {
    /**
     * Search for 5 tickets in one project, and move them
     * to the other project within same Jira instance.
     * When moving tickets, please move following fields:
     * - summary (title) +
     * - description +
     * - priority +
     * Bonus points for syncing comments. +
     */
    public static void main(String[] args) {
        try {
            new JiraSynchronizer().moveTasksToOtherProject();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void moveTasksToOtherProject() throws Exception {
        String instance = ""; //your jira instance for example user.atlassian.net
        String bearerToken = ""; //encoded Base64 email:token
        String from = ""; //project1 full name, for example: GetInt technical interview
        String to = ""; //project2 full name

        if(instance.isEmpty() || instance == null) {
            throw new Exception("Empty instance!");
        }

        if(bearerToken.isEmpty() || bearerToken == null) {
            throw new Exception("Empty Bearer Token!");
        }

        int[] fromTo = getFromTo(instance, bearerToken, from, to); //get boards ids
        if (fromTo[0] == 0 || fromTo[1] == 0) { //return if not found
            throw new Exception("Wrong from/to name!");
        }
        Issue[] issues = getIssues(instance, bearerToken, fromTo[0]); //get 5 issues

        if (issues.length > 0) {
            for (Issue issue : issues) { //create new issues in To project
                Comment[] comments = issue.fields.comment.comments;
                createIssue(instance, bearerToken, fromTo[1], issue.fields.summary, issue.fields.description, issue.fields.priority, comments);
            }

            for (Issue issue : issues) { //remove 5 issues from From project
                deleteIssue(instance, bearerToken, Integer.parseInt(issue.id));
            }
            int length = Math.min(issues.length, 5);
            System.out.println("Thank you for moving " + length + " issues from " + from + " to " + to);
        } else {
            throw new Exception("There are no issues left in " + from + " project");
        }
    }

    public static int[] getFromTo(String instance, String bearerToken, String from, String to) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet("https://" + instance + "/rest/agile/1.0/board");
                httpGet.setHeader("Accept", "application/json");
                httpGet.setHeader("Authorization", "Basic " + bearerToken);

                try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                    HttpEntity entity1 = response1.getEntity();
                    String string = EntityUtils.toString(entity1);
                    JSONObject result = new JSONObject(string);
                    JSONArray array = result.getJSONArray("values");

                    Board[] boards = objectMapper.readValue(array.toString(), Board[].class);
                    int[] fromTo = new int[2];

                    for (Board board : boards) {
                        if (board.location.projectName.equalsIgnoreCase(from)) {
                            fromTo[0] = board.id;
                        }

                        if (board.location.projectName.equalsIgnoreCase(to)) {
                            fromTo[1] = board.location.projectId;
                        }
                    }

                    return fromTo;

                }
            }
        } catch (Exception err) {
            System.out.println(err.getMessage());
        }
        return new int[0];
    }

    public static Issue[] getIssues(String instance, String bearerToken, int id) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet("https://" + instance + "/rest/agile/1.0/board/" + id + "/issue?maxResults=5");
                httpGet.setHeader("Accept", "application/json");
                httpGet.setHeader("Authorization", "Basic " + bearerToken);

                try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                    HttpEntity entity1 = response1.getEntity();
                    String string = EntityUtils.toString(entity1);
                    JSONObject result = new JSONObject(string);
                    JSONArray array = result.getJSONArray("issues");

                    return objectMapper.readValue(array.toString(), Issue[].class);
                }
            }
        } catch (Exception err) {
            System.out.println(err.getMessage());
        }

        return new Issue[0];
    }

    public static void createIssue(String instance, String bearerToken, int id, String summary, String description, Priority priority, Comment[] comments) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost("https://" + instance + "/rest/api/2/issue/");
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Basic " + bearerToken);
                String descriptionText = description != null ? description : "";
                String json = (" {" +
                               "     \"fields\": {" +
                               "        \"project\":" +
                               "        {\n" +
                               "           \"id\": \"" + id + "\"" +
                               "        }," +
                               "        \"summary\": \"" + summary + "\"," +
                               "        \"description\": \"" + descriptionText + "\"," +
                               "        \"issuetype\": {" +
                               "           \"name\": \"Task\"" +
                               "        }," +
                               "        \"priority\": {" +
                               "           \"self\": \"" + priority.self + "\"," +
                               "           \"iconUrl\": \"" + priority.iconUrl + "\"," +
                               "           \"name\": \"" + priority.name + "\"," +
                               "           \"id\": \"" + priority.id + "\"" +
                               "        }\n" +
                               "     }\n" +
                               "}");


                StringEntity entity = new StringEntity(json);
                entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
                httpPost.setEntity(entity);

                try (CloseableHttpResponse response1 = httpclient.execute(httpPost)) {
                    HttpEntity entity1 = response1.getEntity();
                    String string = EntityUtils.toString(entity1);
                    JSONObject result = new JSONObject(string);
                    String newIssueId = result.getString("id");

                    for (Comment comment : comments) { // add comments to issue
                        addCommentToIssue(instance, bearerToken, Integer.parseInt(newIssueId), comment);
                    }

                }
            }
        } catch (Exception err) {
            System.out.println(err.getMessage());
        }
    }

    public static void deleteIssue(String instance, String bearerToken, int id) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpDelete httpDelete = new HttpDelete("https://" + instance + "/rest/api/2/issue/" + id);
            httpDelete.setHeader("Authorization", "Basic " + bearerToken);

            httpclient.execute(httpDelete);
        } catch (Exception err) {
            System.out.println(err.getMessage());
        }
    }

    public static void addCommentToIssue(String instance, String bearerToken, int id, Comment comment) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost("https://" + instance + "/rest/api/2/issue/" + id + "/comment");
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Basic " + bearerToken);
                String json = ("{" +
                               "     \"body\": \"" + comment.body + "\"" +
                               "}");

                StringEntity entity = new StringEntity(json);
                entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
                httpPost.setEntity(entity);

                httpclient.execute(httpPost);
            }
        } catch (Exception err) {
            System.out.println(err.getMessage());
        }
    }
}
