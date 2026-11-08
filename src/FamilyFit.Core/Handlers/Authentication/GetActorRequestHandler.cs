using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Authentication;

public sealed class GetActorRequestHandler(AuthService auth, IMediator mediator)
    : AuthorizedHandler<GetActorRequest, Actor>(auth, mediator)
{
    protected override Task<Outcome<Actor>> Execute(GetActorRequest request, Actor actor, CancellationToken ct) => Task.FromResult(Outcome<Actor>.Ok(actor));
}
