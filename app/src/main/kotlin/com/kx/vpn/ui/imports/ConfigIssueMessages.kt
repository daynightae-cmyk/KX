package com.kx.vpn.ui.imports

import androidx.annotation.StringRes
import com.kx.vpn.R
import com.kx.vpn.domain.config.IssueMessage

/**
 * Maps a parser finding to a localised string.
 *
 * The validator returns enum ids rather than formatted text precisely so this mapping
 * can exist: a message built by interpolating a config value would leak a private key
 * into the UI, a screenshot or a log.
 */
object ConfigIssueMessages {

    @StringRes
    fun of(message: IssueMessage): Int = when (message) {
        IssueMessage.Empty -> R.string.config_issue_empty
        IssueMessage.TooLarge -> R.string.config_issue_too_large
        IssueMessage.TooManyLines -> R.string.config_issue_too_many_lines
        IssueMessage.NotUtf8 -> R.string.config_issue_not_utf8
        IssueMessage.ControlCharacters -> R.string.config_issue_control_characters
        IssueMessage.MissingInterfaceSection -> R.string.config_issue_missing_interface
        IssueMessage.MultipleInterfaceSections -> R.string.config_issue_multiple_interface
        IssueMessage.MissingPeerSection -> R.string.config_issue_missing_peer
        IssueMessage.UnknownSection -> R.string.config_issue_unknown_section
        IssueMessage.MissingKey -> R.string.config_issue_missing_key
        IssueMessage.InvalidBase64Key -> R.string.config_issue_invalid_key
        IssueMessage.WrongKeyLength -> R.string.config_issue_invalid_key
        IssueMessage.InvalidPort -> R.string.config_issue_invalid_port
        IssueMessage.InvalidIpAddress -> R.string.config_issue_invalid_address
        IssueMessage.InvalidCidr -> R.string.config_issue_invalid_cidr
        IssueMessage.InvalidDnsServer -> R.string.config_issue_invalid_dns
        IssueMessage.InvalidEndpoint -> R.string.config_issue_invalid_endpoint
        IssueMessage.DuplicatePeer -> R.string.config_issue_duplicate_peer
        IssueMessage.NoAllowedIps -> R.string.config_issue_no_allowed_ips
        IssueMessage.InvalidKeepalive -> R.string.config_issue_invalid_keepalive
        IssueMessage.InvalidMtu -> R.string.config_issue_invalid_mtu
    }
}
