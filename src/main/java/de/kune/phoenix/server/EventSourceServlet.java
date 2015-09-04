package de.kune.phoenix.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EventSourceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		System.out.println("---> " + req);
		System.out.println("---> " + resp);
		resp.setContentType("text/event-stream");
		resp.setCharacterEncoding("UTF-8");
		String msg = req.getParameter("msg");
		System.out.println("msg---> " + msg);
		
		PrintWriter writer = resp.getWriter();
		while (true) {
			writer.write("data: " + DateFormat.getDateTimeInstance().format(new Date()) + "\n\n");
			writer.flush();
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
