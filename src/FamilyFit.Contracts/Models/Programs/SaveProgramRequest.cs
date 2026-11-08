using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Programs;

public sealed record SaveProgramRequest(ClaimsPrincipal User, Guid? Id, ProgramInput Input) : IRequest<Outcome<WorkoutProgram>>, IAuthorizedRequest, IChangesFamily { public bool AdminOnly => true; }
