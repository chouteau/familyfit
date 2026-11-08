using System.Security.Claims;
using System.Text.Json;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Agents;
using FamilyFit.Core.Services;
using ModelContextProtocol.Protocol;

namespace FamilyFit.Web.Mcp;

public sealed class AgentToolRunner(IMediator mediator, AuthService auth)
{
    public Task<CallToolResult> Read(ClaimsPrincipal user, AgentQuery query, CancellationToken ct)
        => Run(user, () => mediator.GetAgentData(user, query, ct), ct);

    public async Task<CallToolResult> Run<T>(ClaimsPrincipal user, Func<Task<Outcome<T>>> action, CancellationToken ct)
    {
        try
        {
            var actor = await auth.RequireActorAsync(user, ct: ct);
            if (actor.AgentId == null) return Error("Utilise une clé dédiée à l’agent.", 403);
            var result = await action();
            return result.Success ? Result(new { data = result.Value }) : Error(result.Error!, result.Status);
        }
        catch (DomainException error) { return Error(error.Message, error.Status); }
    }

    public static CallToolResult Result(object value)
    {
        var json = JsonSerializer.SerializeToElement(value, JsonDefaults.Options);
        return new() { Content = [new TextContentBlock { Text = json.GetRawText() }], StructuredContent = json, IsError = false };
    }
    public static CallToolResult Error(string message, int status) => new() { Content = [new TextContentBlock { Text = JsonSerializer.Serialize(new { error = message, status }) }], IsError = true };
}
