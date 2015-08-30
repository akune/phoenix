package de.kune.phoenix.shared;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

@Path("/")
public interface MessageService extends RestService {

	@POST
	@Path("message")
	@Consumes(MediaType.APPLICATION_JSON)
	void post(Message message, MethodCallback<Void> callback);

	@POST
	@Path("conversation/{conversation}/message")
	@Consumes(MediaType.APPLICATION_JSON)
	void postToConversation(@PathParam("conversation") String conversationId, Message message,
			MethodCallback<Void> callback);

	@GET
	@Path("message")
	@Produces(MediaType.APPLICATION_JSON)
	void get(@QueryParam("wait") boolean wait, @QueryParam("transmitted-after") Long transmittedAfter,
			@QueryParam("recipient-id") String recipientId, MethodCallback<List<Message>> callback);

	@GET
	@Path("conversation/{conversation}/message")
	@Produces(MediaType.APPLICATION_JSON)
	void getFromConversation(@PathParam("conversation") String conversationId, @QueryParam("wait") boolean wait,
			@QueryParam("transmitted-after") Long transmittedAfter, @QueryParam("recipient-id") String recipientId,
			MethodCallback<List<Message>> callback);

}
