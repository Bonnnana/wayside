package com.wayside.services.interfaces.api

/** Where the API lives. One place to change when the host moves. */
interface ConfigurationService {
    val apiHost: String
    val apiBasePath: String

    /** http only for a local development server, which has no certificate a device would trust. */
    val apiScheme: String

    /** Null means the scheme's standard port. */
    val apiPort: Int?
}
