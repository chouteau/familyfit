using FamilyFit.Contracts.Models.Equipment;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Contracts.Models.Devices;
using FamilyFit.Contracts.Models.Family;
using FamilyFit.Contracts.Models.Locations;
using FamilyFit.Contracts.Models.Members;
using FamilyFit.Contracts.Models.Programs;
using FamilyFit.Contracts.Models.Workouts;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.EntityFrameworkCore;
using QRCoder;

namespace FamilyFit.Web.Services;

public static class ApiEndpoints
{
    public static IResult Http<T>(Outcome<T> result) => result.Success ? Results.Ok(result.Value) : Results.Problem(detail: result.Error, statusCode: result.Status);
    public static void MapFamilyApi(this WebApplication app)
    {
        var api = app.MapGroup("/api/v1");
        api.MapPost("/auth/bootstrap", async (BootstrapInput input, HttpContext context, IMediator mediator, CancellationToken ct) =>
        {
            if (!SameOrigin(context)) return Results.StatusCode(403);
            var result = await mediator.CompleteBootstrap(input, ct);
            if (!result.Success) return Http(result);
            var grant = result.Value!;
            await context.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, AuthService.BootstrapPrincipal(grant.Member, grant.Session.Id), new AuthenticationProperties { ExpiresUtc = grant.Session.ExpiresAtUtc, IsPersistent = false });
            return Results.Ok(new { state = "approved" });
        }).RequireRateLimiting("bootstrap");
        api.MapPost("/auth/enroll", async (EnrollRequest input, IMediator mediator, CancellationToken ct) => Http(await mediator.EnrollDevice(input, ct))).Produces<DeviceGrant>().RequireRateLimiting("pairing");
        api.MapPost("/auth/invitations/preview", async (ApproveRequest input, IMediator mediator, CancellationToken ct) => Http(await mediator.PreviewInvitation(input.Code, ct))).RequireRateLimiting("pairing");
        api.MapPost("/auth/browser/challenges", async (BrowserChallengeInput input, HttpContext context, IMediator mediator, CancellationToken ct) =>
        {
            if (!SameOrigin(context)) return Results.StatusCode(403);
            var cookie = context.Request.Cookies["FamilyFit.Challenge"]?.Split(':', 2);
            var result = await mediator.CreateBrowserChallenge(input.LinkSecret, cookie?.Length == 2 ? cookie[1] : null, ct);
            if (!result.Success) return Http(result);
            var value = result.Value!;
            context.Response.Cookies.Append("FamilyFit.Challenge", $"{value.Grant.Id}:{value.BrowserSecret}", ChallengeCookie(context));
            using var data = QRCodeGenerator.GenerateQrCode(value.Grant.QrPayload, QRCodeGenerator.ECCLevel.M);
            return Results.Ok(new { value.Grant.Id, value.Grant.Code, value.Grant.ExpiresAtUtc, qrSvg = new SvgQRCode(data).GetGraphic(5) });
        }).RequireRateLimiting("pairing");
        api.MapPost("/auth/browser/consume", async (HttpContext context, IMediator mediator, CancellationToken ct) =>
        {
            if (!SameOrigin(context)) return Results.StatusCode(403);
            var cookie = context.Request.Cookies["FamilyFit.Challenge"]?.Split(':', 2);
            if (cookie?.Length != 2 || !Guid.TryParse(cookie[0], out var id)) return Results.Unauthorized();
            var result = await mediator.ConsumeBrowserChallenge(id, cookie[1], ct);
            if (!result.Success) return Http(result);
            if (result.Value!.Session is { } session)
            {
                await context.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, AuthService.Principal(result.Value.Member!, session.DeviceId!.Value, session.Id), new AuthenticationProperties { ExpiresUtc = session.ExpiresAtUtc, IsPersistent = false });
                context.Response.Cookies.Delete("FamilyFit.Challenge");
            }
            return Results.Ok(new { state = result.Value.State });
        }).RequireRateLimiting("polling");
        api.MapPost("/auth/browser/approve", async (ApproveRequest input, HttpContext context, IMediator mediator, CancellationToken ct) => Http(await mediator.ApproveBrowserLogin(context.User, input.Code, ct))).RequireAuthorization("DeviceOnly").RequireRateLimiting("pairing");
        api.MapPost("/auth/logout", async (HttpContext context, IMediator mediator, CancellationToken ct) =>
        {
            if (!SameOrigin(context)) return Results.StatusCode(403);
            var result = await mediator.CloseBrowserSession(context.User, ct);
            if (!result.Success) return Http(result);
            await context.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            return Results.Ok();
        }).RequireAuthorization();
        api.MapGet("/me", async (HttpContext context, IMediator mediator, CancellationToken ct) => Http(await mediator.GetActor(context.User, cancellationToken: ct))).RequireAuthorization();
        api.MapGet("/family", async (HttpContext context, IMediator mediator, CancellationToken ct) => Http(await mediator.GetDashboard(context.User, ct))).Produces<DashboardData>().RequireAuthorization();
        api.MapGet("/sync", async (HttpContext context, IMediator mediator, CancellationToken ct) => Http(await mediator.GetSync(context.User, ct))).Produces<SyncSnapshot>().RequireAuthorization("DeviceOnly");
        api.MapPost("/workouts", async (WorkoutSubmission input, HttpContext context, IMediator mediator, CancellationToken ct) => Http(await mediator.SubmitWorkout(context.User, input, ct))).Produces<WorkoutResultView>().RequireAuthorization("DeviceOnly");
        // Mobile mutations use bearer authentication; browser management runs through authenticated Blazor handlers.
        api.MapPost("/members", async (MemberInput input, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.SaveMember(c.User, null, input, ct))).RequireAuthorization("DeviceOnly");
        api.MapPut("/members/{id:guid}", async (Guid id, MemberInput input, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.SaveMember(c.User, id, input, ct))).RequireAuthorization("DeviceOnly");
        api.MapPost("/locations", async (LocationInput input, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.SaveLocation(c.User, null, input, ct))).RequireAuthorization("DeviceOnly");
        api.MapPut("/locations/{id:guid}", async (Guid id, LocationInput input, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.SaveLocation(c.User, id, input, ct))).RequireAuthorization("DeviceOnly");
        api.MapPost("/members/{id:guid}/invitation", async (Guid id, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.CreateInvitation(c.User, id, ct))).RequireAuthorization("DeviceOnly");
        api.MapPost("/programs", async (ProgramInput input, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.SaveProgram(c.User, null, input, ct))).RequireAuthorization("DeviceOnly");
        api.MapPut("/programs/{id:guid}", async (Guid id, ProgramInput input, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.SaveProgram(c.User, id, input, ct))).RequireAuthorization("DeviceOnly");
        api.MapPost("/programs/{id:guid}/publish", async (Guid id, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.PublishProgram(c.User, id, ct))).RequireAuthorization("DeviceOnly");
        api.MapGet("/devices", async (HttpContext c, IMediator m, CancellationToken ct) => Http(await m.GetDevices(c.User, ct))).RequireAuthorization();
        api.MapPost("/devices/{id:guid}/revoke", async (Guid id, HttpContext c, IMediator m, CancellationToken ct) => Http(await m.RevokeDevice(c.User, id, ct))).RequireAuthorization("DeviceOnly");
        app.MapGet("/media/equipment/{id}", async (string id, HttpContext context, IMediator mediator, CancellationToken ct) =>
        {
            var result = await mediator.GetEquipmentImage(context.User, id, ct);
            return result.Success ? Results.File(result.Value!.Data, result.Value.ContentType) : Http(result);
        }).RequireAuthorization();
        app.MapGet("/media/{file}", (string file, IWebHostEnvironment environment) =>
        {
            if (Path.GetFileName(file) != file || !new[] { ".jpg", ".png", ".mp4" }.Contains(Path.GetExtension(file).ToLowerInvariant())) return Results.NotFound();
            var path = Path.Combine(environment.ContentRootPath, "CatalogueMedia", file);
            return File.Exists(path) ? Results.File(path, Path.GetExtension(file) == ".mp4" ? "video/mp4" : Path.GetExtension(file) == ".png" ? "image/png" : "image/jpeg", enableRangeProcessing: true) : Results.NotFound();
        }).RequireAuthorization();
    }
    private static bool SameOrigin(HttpContext context)
    {
        if (context.Request.Headers["X-FamilyFit-Request"] != "1") return false;
        var site = context.Request.Headers["Sec-Fetch-Site"].ToString();
        if (site is "cross-site" or "same-site") return false;
        var origin = context.Request.Headers.Origin.ToString();
        return origin.Length == 0 || (Uri.TryCreate(origin, UriKind.Absolute, out var uri) && uri.Authority.Equals(context.Request.Host.Value, StringComparison.OrdinalIgnoreCase) && uri.Scheme == context.Request.Scheme);
    }
    private static CookieOptions ChallengeCookie(HttpContext c) => new() { HttpOnly = true, Secure = c.Request.IsHttps, SameSite = SameSiteMode.Strict, MaxAge = TimeSpan.FromMinutes(5), Path = "/" };
}
