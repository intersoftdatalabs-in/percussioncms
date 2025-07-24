/**
 * Implementation of the email request interface.
 */
public class PSEmailRequest implements IPSEmailRequest {
	private String toList;
	private String ccList;
	private String bodycontent;
	private String subject;
	private String bccList;

	@Override
	public void setToList(String toList) {
		this.toList = toList;
	}

	@Override
	public void setCCList(String ccList) {
		this.ccList = ccList;
	}

	@Override
	public void setBody(String bodycontent) {
		this.bodycontent = bodycontent;
	}

	@Override
	public void setSubject(String subject) {
		this.subject = subject