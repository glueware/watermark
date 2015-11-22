# Watermark


# Implementation

There are three packages:

* `controllers`
* `actors`
* `models`

The idea is to map the functionality of a semi-structured outside world (the web service interface) to a well typed "logic" world. That means the `controllers` itself do not contain any "logic", even error handling and validation is done within the `actors` package. The types of the "logic" world are defined in the models.

## Process
 

## Models
Models are case classes. we have two models:
* `Document`
* `Ticket`

### Document
`Document` is the result of 



