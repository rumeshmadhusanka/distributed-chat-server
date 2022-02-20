package InboundRequest;

import org.json.simple.JSONObject;

public class InboundRequest {
    private JSONObject jsonPayload;

    public InboundRequest(JSONObject jsonPayload){
        this.jsonPayload = jsonPayload;
    }


}
