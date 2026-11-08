using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Programs;

public static class ProgramsExtensions
{
    public static Task<Outcome<WorkoutProgram>> SaveProgram(this IMediator mediator, ClaimsPrincipal User, Guid? Id, ProgramInput Input, CancellationToken cancellationToken = default) => mediator.Send(new SaveProgramRequest(User, Id, Input), cancellationToken);
    public static Task<Outcome<WorkoutProgram>> PublishProgram(this IMediator mediator, ClaimsPrincipal User, Guid Id, CancellationToken cancellationToken = default) => mediator.Send(new PublishProgramRequest(User, Id), cancellationToken);
}
