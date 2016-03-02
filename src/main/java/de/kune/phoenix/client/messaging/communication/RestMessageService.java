package de.kune.phoenix.client.messaging.communication;

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

import de.kune.phoenix.shared.Message;

@Path("/")
public interface RestMessageService extends RestService {
	@POST
	@Path("message")
	@Consumes(MediaType.APPLICATION_JSON)
	void post(List<Message> messages, MethodCallback<Void> callback);

	@GET
	@Path("message")
	@Produces(MediaType.APPLICATION_JSON)
	void get(@QueryParam("wait") boolean wait, @QueryParam("last-sequence-key") String lastSequenceKey,
			@QueryParam("recipient-id") String recipientId, MethodCallback<List<Message>> callback);
	
	@GET
	@Path("message/{messageId}")
	@Produces(MediaType.APPLICATION_JSON)
	void get(@PathParam("messageId") String messageId, MethodCallback<Message> callback);

}