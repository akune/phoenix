package de.kune.phoenix.client.messaging;

import com.google.gwt.core.client.GWT;
import de.kune.phoenix.client.functional.FailureHandler;
import de.kune.phoenix.client.functional.SuccessHandler;
import de.kune.phoenix.client.messaging.communication.RestIdentityService;
import de.kune.phoenix.shared.Identity;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

public class IdentityService {

    private static final IdentityService instance = new IdentityService();

    public static IdentityService instance() {return instance;}

    private RestIdentityService restIdentityService;

    private IdentityService() {
        restIdentityService = GWT.create(RestIdentityService.class);
    }

    public void receiveIdentity(String identity, SuccessHandler<Identity> successHandler, FailureHandler<Void> failureHandler) {
        restIdentityService.get(identity, new MethodCallback<Identity>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                failureHandler.handle(exception, null);
            }

            @Override
            public void onSuccess(Method method, Identity response) {
                successHandler.handle(response);
            }
        });
    }

    public void sendIdentity(Identity identity) {
        restIdentityService.post(identity, new MethodCallback<Void>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                GWT.log("Could not send identity");
            }

            @Override
            public void onSuccess(Method method, Void response) {
                GWT.log("Sent identity");
            }
        });
    }
}
