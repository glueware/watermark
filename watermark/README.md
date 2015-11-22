# Watermark
A global publishing company that publishes books and journals wants to develop a service to 
watermark their documents. Book publications include topics in business, science and media. Journals 
don’t include any specific topics.

# Implementation

There are three packages:
* `controllers`
* `models`
* `actors`

## Package `controllers`
The idea is to map the functionality of a semi-structured outside world (the web service interface) to a well typed "logic" world. That means the `controllers` itself do not contain any "logic", even error handling and validation is done within the `actors` package. The types of the "logic" world are defined in the models. The mapping of the errors is done in similar generic as the mapping of the types.

The web services are implemented by using the play framework. The routing is designed as close as possible to a RESTful API.

## Package `models`
Models are case classes (immutable) which may have a common `trait`as superclass. Models are used for message exchange with the external world via web services and with internal actor messages. 
* `Document` A document (books, journals) has a title, author and a watermark property. An empty watermark property indicates that the document has not been watermarked yet.
* `Book` A subclass of Document with additional property topic.
* `Journal` A subclass of Document with no additional property.
* `Ticket` A ticket can be used to poll the status of processing. So the ticket has an id and a status.

## Package `actors`
The watermark actor does (simulates) the work. It receives messages with the content document and produces a watermark.   

# Process
The web service API may be used as follows:
* `accept`: A given content document is delivered to the web service and sent to the watermark actor and returns a ticket.
* `status`: The id of the ticket may be sent to the server in order to retrieve a new ticket with a new status.
* `retrieve`: If the status of the ticket is `processed` one may retrieve the document including its watermark with the ticket id. Otherwise the watermark may be empty.  




