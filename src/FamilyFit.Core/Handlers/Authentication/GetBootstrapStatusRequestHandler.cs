using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;
namespace FamilyFit.Core.Handlers.Authentication;
public sealed class GetBootstrapStatusRequestHandler(BootstrapService bootstrap) : IRequestHandler<GetBootstrapStatusRequest, Outcome<BootstrapStatus>>
{
    public async Task<Outcome<BootstrapStatus>> Handle(GetBootstrapStatusRequest request, CancellationToken ct) => Outcome<BootstrapStatus>.Ok(await bootstrap.StatusAsync(ct));
}
