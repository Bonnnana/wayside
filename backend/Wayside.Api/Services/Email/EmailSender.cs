using MailKit.Net.Smtp;
using MailKit.Security;
using Microsoft.Extensions.Options;
using MimeKit;
using Wayside.Api.Configuration;

namespace Wayside.Api.Services.Email;

public interface IEmailSender
{
    Task SendPasswordResetAsync(
        string toAddress,
        string resetToken,
        CancellationToken cancellationToken = default);
}

/// <summary>
/// Sends over SMTP when <see cref="EmailOptions.Enabled"/> is set; otherwise logs the token so
/// the reset flow is testable without SMTP credentials.
/// </summary>
public sealed class SmtpEmailSender(
    IOptions<EmailOptions> options,
    ILogger<SmtpEmailSender> logger) : IEmailSender
{
    private readonly EmailOptions _options = options.Value;

    public async Task SendPasswordResetAsync(
        string toAddress,
        string resetToken,
        CancellationToken cancellationToken = default)
    {
        if (!_options.Enabled || string.IsNullOrWhiteSpace(_options.Host))
        {
            // Deliberately logged at Warning so it stands out in the console during development.
            logger.LogWarning(
                "Email disabled. Password reset token for {Email}: {Token}",
                toAddress,
                resetToken);
            return;
        }

        var message = new MimeMessage();
        message.From.Add(new MailboxAddress(_options.FromName, _options.FromAddress));
        message.To.Add(MailboxAddress.Parse(toAddress));
        message.Subject = "Reset your Wayside password";
        message.Body = new BodyBuilder
        {
            TextBody =
                $"""
                Someone asked to reset the password for this Wayside account.

                Your reset code is:

                    {resetToken}

                Enter it in the app to choose a new password. The code expires in a few minutes.

                If this wasn't you, ignore this email — nothing has changed.
                """,
        }.ToMessageBody();

        using var client = new SmtpClient();
        try
        {
            await client.ConnectAsync(
                _options.Host, _options.Port, SecureSocketOptions.StartTls, cancellationToken);
            await client.AuthenticateAsync(_options.Username, _options.Password, cancellationToken);
            await client.SendAsync(message, cancellationToken);
        }
        finally
        {
            if (client.IsConnected)
            {
                await client.DisconnectAsync(true, cancellationToken);
            }
        }
    }
}
