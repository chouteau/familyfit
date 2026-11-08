using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;
namespace FamilyFit.Core.Handlers.Authentication;
public sealed class CompleteBootstrapRequestHandler(BootstrapService bootstrap) : IRequestHandler<CompleteBootstrapRequest, Outcome<BootstrapGrant>>
{
    public async Task<Outcome<BootstrapGrant>> Handle(CompleteBootstrapRequest request, CancellationToken ct)
    {
        try { return Outcome<BootstrapGrant>.Ok(await bootstrap.CompleteAsync(request.Input, ct)); }
        catch (DomainException ex) { return Outcome<BootstrapGrant>.Fail(ex.Message, ex.Status); }
    }
}
