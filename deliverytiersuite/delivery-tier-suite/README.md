# delivery-tier-suite
This module contains submodules for each of the DTS Services.
* Caching Service
* Comments Service
* Feeds Service
* Forms Service
* Membership Service
* Metadata Service
* Polls Service

And all other supporting services modules. 

## Building

```
mvn clean install
```

# Delivery Tier Suite - Integrations Module

## Java 11 Migration & SOAP Modernization

All SOAP request/response classes in `service.web.api.ems.dea` have been refactored to Java 11 standards:
- Uses `Optional`, builder pattern, and immutable data structures.
- Follows Google Java Style Guide.
- Classes are thread-safe, OWASP-compliant, and use concise Javadoc.
- All public APIs remain backward compatible.

### Refactored Classes
- `AddReservationResponse.java`
- `AddReservation2Response.java`
- `AddReservation3Response.java`
- `AddReservation4Response.java`
- `AddReservation5Response.java`
- `AddReservation4.java`

### Usage Example

```java
// Construct a AddReservation4 request
var request = new AddReservation4.Builder()
    .userName("admin")
    .password("secret")
    .groupID(1)
    .roomID(101)
    .bookingDate(bookingDate)
    .startTime(startTime)
    .endTime(endTime)
    .eventName("Board Meeting")
    .statusID(2)
    .eventTypeID(3)
    .webUserID(4)
    .webTemplateID(5)
    .reservationSourceID(6)
    .billingReference("BR-2024")
    .build();

// Access response result safely
var result = response.getAddReservation4Result().orElse("No reservation created");
```

### Migration Notes

- All SOAP classes now use builder pattern for instantiation.
- All getters return `Optional` for null safety.
- Password fields are protected in `toString()`.
- No breaking changes to public interfaces.

### Tracking

- `service.web.api.ems.dea` added to `refactored-java11-packages.txt`
- `service.web.api.ems.dea` added to `refactored-soap-packages.txt`

---

< I'll be back... with cleaner code >
_______________________________
< May the force of Java 11 be with you! >
---------------------------------
        \   ^__^
         \  (oo)\_______
            (__)\\       )\/\
                ||----w |
                ||     ||
