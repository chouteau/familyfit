using System.Security.Claims;
using ChannelMediator;

namespace FamilyFit.Contracts.Models.Agents;

public static class AgentsExtensions
{
    public static Task<Outcome<object>> GetAgentData(this IMediator mediator, ClaimsPrincipal user, AgentQuery query, CancellationToken cancellationToken = default)
        => mediator.Send(new GetAgentDataRequest(user, query), cancellationToken);
}
