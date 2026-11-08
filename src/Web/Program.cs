using System.Net;
using System.Security.Claims;
using System.Threading.RateLimiting;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Core.Data;
using FamilyFit.Core.Handlers.Family;
using FamilyFit.Core.Services;
using FamilyFit.Web.Components;
using FamilyFit.Web.Services;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.AspNetCore.DataProtection;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.EntityFrameworkCore;
using FamilyFit.Web.Mcp;
using ModelContextProtocol.AspNetCore;

if (args.Contains("--healthcheck"))
{
    try { using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(4) }; using var response = await client.GetAsync("http://127.0.0.1:8080/health"); Environment.ExitCode = response.IsSuccessStatusCode ? 0 : 1; }
    catch (HttpRequestException) { Environment.ExitCode = 1; }
    catch (TaskCanceledException) { Environment.ExitCode = 1; }
    return;
}
var builder = WebApplication.CreateBuilder(args);
var dataDirectory = Path.GetFullPath(builder.Configuration["FamilyFit:DataDirectory"] ?? Path.Combine(builder.Environment.ContentRootPath, "data"));
Directory.CreateDirectory(dataDirectory);
builder.Configuration["FamilyFit:DataDirectory"] = dataDirectory;
var demo = builder.Configuration.GetValue<bool>("FamilyFit:Demo");
if (demo && !builder.Environment.IsDevelopment()) throw new InvalidOperationException("Le mode démonstration est interdit en production.");
builder.Services.AddDbContextFactory<FamilyDb>((services, options) =>
{
    var config = services.GetRequiredService<IConfiguration>();
    var directory = Path.GetFullPath(config["FamilyFit:DataDirectory"]!);
    Directory.CreateDirectory(directory);
    var database = Path.Combine(directory, config.GetValue<bool>("FamilyFit:Demo") ? "familyfit-demo.db" : "familyfit.db");
    options.UseSqlite($"Data Source={database};Foreign Keys=True;Default Timeout=15");
});
builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<FamilyUpdates>();
builder.Services.AddScoped<AgentAuthorization>();
builder.Services.AddScoped<AgentToolRunner>();
builder.Services.AddHttpContextAccessor();
builder.Services.AddMcpServer(options => options.ServerInstructions = FamilyReadTools.Instructions)
    .WithHttpTransport(options => options.SessionMode = HttpServerSessionMode.Stateless)
    .WithTools<FamilyReadTools>().WithTools<FamilyWriteTools>();
builder.Services.AddScoped<AuthService>();
builder.Services.AddScoped<BootstrapService>();
builder.Services.AddChannelMediator(assemblies: [typeof(GetDashboardRequestHandler).Assembly]);
builder.Services.AddRazorComponents().AddInteractiveServerComponents();
builder.Services.AddCascadingAuthenticationState();
builder.Services.AddScoped<AuthenticationStateProvider, FamilyAuthenticationStateProvider>();
builder.Services.AddDataProtection().PersistKeysToFileSystem(new DirectoryInfo(Path.Combine(dataDirectory, "keys"))).SetApplicationName("FamilyFit");
builder.Services.AddAuthentication(options => { options.DefaultScheme = "FamilyFit"; options.DefaultChallengeScheme = "FamilyFit"; })
    .AddPolicyScheme("FamilyFit", null, options => options.ForwardDefaultSelector = context => context.Request.Path.StartsWithSegments("/mcp") ? "Agent" : context.Request.Headers.Authorization.ToString().StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase) ? "Device" : CookieAuthenticationDefaults.AuthenticationScheme)
    .AddScheme<AuthenticationSchemeOptions, DeviceAuthenticationHandler>("Device", null)
    .AddScheme<AuthenticationSchemeOptions, AgentAuthenticationHandler>("Agent", null)
    .AddCookie(options =>
    {
        options.Cookie.Name = "FamilyFit.Session"; options.Cookie.HttpOnly = true; options.Cookie.SameSite = SameSiteMode.Lax;
        options.Cookie.SecurePolicy = builder.Environment.IsDevelopment() ? CookieSecurePolicy.SameAsRequest : CookieSecurePolicy.Always;
        options.ExpireTimeSpan = TimeSpan.FromHours(12); options.SlidingExpiration = false; options.LoginPath = "/login";
        options.Events.OnValidatePrincipal = async context =>
        {
            try { await context.HttpContext.RequestServices.GetRequiredService<AuthService>().RequireActorAsync(context.Principal!); }
            catch (DomainException) { context.RejectPrincipal(); await context.HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme); }
        };
        options.Events.OnRedirectToLogin = context => { if (context.Request.Path.StartsWithSegments("/api") || context.Request.Path.StartsWithSegments("/media")) context.Response.StatusCode = 401; else context.Response.Redirect(context.RedirectUri); return Task.CompletedTask; };
    });
builder.Services.AddAuthorization(options =>
{
    options.AddPolicy("DeviceOnly", policy => policy.AddAuthenticationSchemes("Device").RequireAuthenticatedUser().RequireClaim("method", "device"));
    options.AddPolicy("AgentOnly", policy => policy.AddAuthenticationSchemes("Agent").RequireAuthenticatedUser().RequireClaim("method", "agent"));
});
builder.Services.AddOpenApi();
builder.Services.AddHealthChecks().AddCheck<DatabaseHealthCheck>("sqlite");
builder.Services.Configure<ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto;
    foreach (var address in builder.Configuration.GetSection("ReverseProxy:KnownProxies").Get<string[]>() ?? [])
        options.KnownProxies.Add(IPAddress.Parse(address));
});
builder.Services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = 429;
    options.AddPolicy("bootstrap", context => RateLimitPartition.GetFixedWindowLimiter(context.Connection.RemoteIpAddress?.ToString() ?? "unknown", _ => new FixedWindowRateLimiterOptions { PermitLimit = 5, Window = TimeSpan.FromMinutes(1), QueueLimit = 0 }));
    options.AddPolicy("mcp", context => RateLimitPartition.GetFixedWindowLimiter(context.Connection.RemoteIpAddress?.ToString() ?? "unknown", _ => new FixedWindowRateLimiterOptions { PermitLimit = 240, Window = TimeSpan.FromMinutes(1), QueueLimit = 0 }));
    options.AddPolicy("pairing", context => RateLimitPartition.GetFixedWindowLimiter(context.Connection.RemoteIpAddress?.ToString() ?? "unknown", _ => new FixedWindowRateLimiterOptions { PermitLimit = 30, Window = TimeSpan.FromMinutes(1), QueueLimit = 0 }));
    options.AddPolicy("polling", context => RateLimitPartition.GetFixedWindowLimiter(context.Connection.RemoteIpAddress?.ToString() ?? "unknown", _ => new FixedWindowRateLimiterOptions { PermitLimit = 120, Window = TimeSpan.FromMinutes(1), QueueLimit = 0 }));
});

var app = builder.Build();
app.UseForwardedHeaders();
if (!app.Environment.IsDevelopment()) { app.UseExceptionHandler("/error", createScopeForErrors: true); app.UseHsts(); }
app.Use(async (context, next) =>
{
    context.Response.Headers["X-Content-Type-Options"] = "nosniff";
    context.Response.Headers["Referrer-Policy"] = "no-referrer";
    context.Response.Headers["X-Frame-Options"] = "DENY";
    if (context.Request.Path.StartsWithSegments("/api") || context.Request.Path.StartsWithSegments("/media") || context.Request.Path.StartsWithSegments("/mcp") || context.Request.Path == "/login") context.Response.Headers.CacheControl = "no-store";
    if (context.Request.Path.StartsWithSegments("/mcp") && context.Request.Headers.Origin.Count > 0)
    {
        var configured = new Uri(builder.Configuration["FamilyFit:PublicUrl"] ?? "https://fit.chouteau.info").GetLeftPart(UriPartial.Authority);
        if (!string.Equals(context.Request.Headers.Origin.ToString(), configured, StringComparison.OrdinalIgnoreCase)) { context.Response.StatusCode = 403; return; }
    }
    try { await next(); }
    catch (DomainException ex) when (!context.Response.HasStarted) { await Results.Problem(detail: ex.Message, statusCode: ex.Status).ExecuteAsync(context); }
});
app.UseRateLimiter();
app.UseAuthentication(); app.UseAuthorization(); app.UseAntiforgery();
app.MapStaticAssets();
app.MapHealthChecks("/health");
app.MapFamilyApi();
app.MapMcp("/mcp").RequireAuthorization("AgentOnly").RequireRateLimiting("mcp");
app.MapRazorComponents<App>().AddInteractiveServerRenderMode();
app.MapOpenApi().RequireAuthorization();

await using (var scope = app.Services.CreateAsyncScope())
{
    var factory = scope.ServiceProvider.GetRequiredService<IDbContextFactory<FamilyDb>>();
    await using var db = await factory.CreateDbContextAsync();
    if (args.Contains("--backup")) { await OperatorCommands.TryRunAsync(args, scope.ServiceProvider, app.Environment); return; }
    await db.Database.MigrateAsync();
    await db.Database.ExecuteSqlRawAsync("PRAGMA journal_mode=WAL;");
    await CatalogueSeeder.SeedAsync(db);
    if (await OperatorCommands.TryRunAsync(args, scope.ServiceProvider, app.Environment)) return;
    if (demo) await DemoSeeder.SeedAsync(db, scope.ServiceProvider.GetRequiredService<TimeProvider>());
}

if (demo)
{
    app.MapGet("/demo", async (HttpContext context, IDbContextFactory<FamilyDb> factory, TimeProvider clock) =>
    {
        await using var db = await factory.CreateDbContextAsync();
        var member = await db.Members.FirstAsync(x => x.IsAdmin);
        var device = await db.Devices.FirstAsync(x => x.MemberId == member.Id);
        var session = new FamilyFit.Contracts.Models.BrowserSession { MemberId = member.Id, DeviceId = device.Id, ExpiresAtUtc = clock.GetUtcNow().AddHours(12) };
        db.BrowserSessions.Add(session); await db.SaveChangesAsync();
        await context.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, AuthService.Principal(member, device.Id, session.Id), new AuthenticationProperties { ExpiresUtc = session.ExpiresAtUtc });
        return Results.LocalRedirect("/");
    });
}
await app.RunAsync();
public partial class Program;
