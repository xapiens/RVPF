# RVPF
Related Values Processing Framework

Overview

The Related Values Processing Framework is being developed to help the integration of process control data historian systems. This is done by providing support for the conversion, computation and distribution of data point values between these systems.

The software is written in Java. All libraries needed to use this software are available under an open source license.

The support for commercial data historian systems WILL NOT be included in this project. Instead, documented interfaces for the integration of such support will be provided. The framework does include a data historian capable enough to support all of its functions.

Since this is a framework for a very technical subject, it is expected that its users will be integrators with expert level knowledge and technical skills. 

Context

The focus of this project is on the circulation and processing of manufacturing points data. By point data, we mean a reference to a point definition, a timestamp, a state and a value. The point definition specifies where the data comes from, where is is stored, what it contains, what effect it may have on other points data, etc. The timestamp indicates when the contained value was generated. The state qualifies the value. The value can be a count, a floating point value, a text string, or anything acceptable at the point of storage.


The points data will usually come from some SCADA (Supervisory Control And Data Acquisition) or DCS (Distributed Control System) implementations. It is assumed that time-critical responses have already been provided by lower level systems. 

Features

    Free and open source software.
    Flexible point dependencies behaviors.
    Unlimited number of point dependencies levels.
    Distributed processing.
    Event triggered recalculations.
    Update anywhere in included data historian.
    Store and forward facilities.
    Files include facility in configuration and metadata.
    Property substitution in configuration and metadata.
    Extensible vocabulary in RPN and Summary engines.
    Replicator engine.
    Transaction based processing to avoid event loss.
    Version control (Subversion) support for metadata.
    Microsecond time support.
    Past (down to 12754 B.C.) to future (up to 16472 A.D.) time support.
    Secure configuration of inter-process communications.
    JMX support.
    JNLP support in background service (subproject).
    Step filter engine.
    Resynchronizer engine.
    Not OS dependent (requires Java 8).
    Flexible extension facilities.
    Multiple components in same or separate JVM.
    JDBC driver for access to point values (subproject).
    Facilities for interfacing external subsystems via RMI, HTTP, file directories or C API.
    Extensive logging and traces.
    Batch processing and internal cache for improved performance.
    Reasonable defaults for a large number of configuration properties.
    Shared or separate configuration files.
    Metadata server.
    Inter/extra-polation supplied by the data historian interface.

Author

    Serge Brisson

Contributors 
