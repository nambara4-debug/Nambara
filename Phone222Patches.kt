package nambara.morphe.patches.googlephone

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

private val googlePhone222Compatibility = Compatibility(
    name = "Google Phone",
    packageName = "com.google.android.dialer",
    appIconColor = 0x1A73E8,
    targets = listOf(
        AppTarget("222.0.916975595"),
    ),
)

@Suppress("unused")
val enableCallRecording222Patch = bytecodePatch(
    name = "Enable call recording (Google Phone 222)",
    description = "Enables the built-in Google Phone call recorder on version 222.0.916975595 by " +
        "bypassing the client-side country availability gate.",
) {
    compatibleWith(googlePhone222Compatibility)

    execute {
        // Verified against Google Phone 222.0.916975595:
        // the CanRecord class still contains this diagnostic string and has exactly one
        // no-argument boolean availability method.
        val canRecord = classDefByStrings(
            "Call recording is disabled in the current country",
        ).singleOrNull()
            ?: throw PatchException(
                "Google Phone 222: CanRecord class not found or ambiguous.",
            )

        val mutableCanRecord = mutableClassDefBy(canRecord)

        val availability = mutableCanRecord.methods.filter { method ->
            method.returnType == "Z" && method.parameterTypes.isEmpty()
        }

        if (availability.size != 1) {
            throw PatchException(
                "Google Phone 222: expected one no-arg boolean CanRecord method, found " +
                    "${availability.size}.",
            )
        }

        availability.single().addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}

@Suppress("unused")
val enableAutomaticCallRecording222Patch = bytecodePatch(
    name = "Enable automatic call recording (Google Phone 222)",
    description = "Unlocks the automatic call-recording availability gate on Google Phone " +
        "222.0.916975595. It also applies the manual call-recording patch as a dependency.",
) {
    dependsOn(enableCallRecording222Patch)
    compatibleWith(googlePhone222Compatibility)

    execute {
        // Verified against Google Phone 222.0.916975595:
        // the Geofence class contains this unique marker and has exactly one no-argument
        // boolean method. CanRecord.canRecordAutomatically() calls both CanRecord and this gate.
        val geofence = classDefByStrings(
            "withinCallRecordingGeoFence",
        ).singleOrNull()
            ?: throw PatchException(
                "Google Phone 222: call-recording Geofence class not found or ambiguous.",
            )

        val mutableGeofence = mutableClassDefBy(geofence)

        val withinGeofence = mutableGeofence.methods.filter { method ->
            method.returnType == "Z" && method.parameterTypes.isEmpty()
        }

        if (withinGeofence.size != 1) {
            throw PatchException(
                "Google Phone 222: expected one no-arg boolean Geofence method, found " +
                    "${withinGeofence.size}.",
            )
        }

        withinGeofence.single().addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
